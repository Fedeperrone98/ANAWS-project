import netsquid as ns
import math


# 1 - creo una connessione con un EPS nel mezzo
def get_EPS_connection(p_m, p_lr, t_clock, length):
    """
    This function returns a netsquid connection having an EPS in the middle.
    """
    conn = ns.nodes.Connection("EPS")

    # determine the states to be emitted --> bell state
    state_sampler = ns.qubits.StateSampler([ns.qubits.ketstates.b00, None], [p_m, 1-p_m])

    # create the EPS
    eps = ns.components.qsource.QSource(name="EPS", state_sampler=state_sampler,
                                        frequency=10e9/t_clock,
                                        num_ports=2, # numero di fotoni da emettere
                                        status=ns.components.qsource.SourceStatus.OFF) # all'inizio della simulazione il QSource è disabilitato

    # create the left and right channels
    delay_model = ns.components.models.FibreDelayModel() # il elay verrà calcolato in base allo specifico valore di lenght
    # netsquid ha una classe specifica per modellare la perdita di segnale della fibra ottica
    loss_model = ns.components.models.FibreLossModel(p_loss_init=1. - p_lr, p_loss_length=0.)
    noise_model = ns.components.models.DepolarNoiseModel(depolar_rate=0.1, time_independent=True)
    models = {"delay_model": delay_model, "quantum_loss_model": loss_model, "quantum_noise_model": noise_model}
    left_channel = ns.components.qchannel.QuantumChannel("left_channel", models=models, length=length/2)
    right_channel = ns.components.qchannel.QuantumChannel("right_channel", models=models, length=length/2)

    # add components to the connection
    conn.add_subcomponent(component=eps, name="EPS")
    conn.add_subcomponent(component=left_channel, name="left_channel", forward_output=[("A", "recv")])
    conn.add_subcomponent(component=right_channel, name="right_channel", forward_output=[("B", "recv")])

    # connect the EPS to the channels
    eps.ports["qout0"].connect(left_channel.ports["send"])
    eps.ports["qout1"].connect(right_channel.ports["send"])

    return conn


# 3 - definisco il MS protocol
class MSProtocol(ns.protocols.NodeProtocol):
    """
    This class implements the midpoint source protocol.
    """

    ENTANGLED_SIGNAL = "new_entangled_pair"
    ENTANGLED_EVT_TYPE = ns.pydynaa.EventType("new_entangled_pair", "A new entangled pair has been delivered")

    def __init__(self, node, K_attempts, t_clock, link_length, connection=None, mem_position=0):
        super().__init__(node=node, name=f"MSProtocol_{mem_position}")
        self.K_attempts = K_attempts
        self.connection = connection
        self.t_clock = t_clock
        self.length = link_length
        self.mem_position = mem_position

        self.add_signal(self.ENTANGLED_SIGNAL, self.ENTANGLED_EVT_TYPE)

    def run(self):

        print(f"[{ns.sim_time()}] Repeater {self.node.ID}: Starting MS Protocol instance")

        # turn on the EPS (NOTE: This is a simulation cheat)
        if self.connection is not None:
            self.connection.subcomponents["EPS"].status = ns.components.qsource.SourceStatus.INTERNAL
            print(f"[{ns.sim_time()}] Repeater {self.node.ID}: Starting EPS")

            # synchronize with the EPS
            yield self.await_port_input(self.node.ports["q0"])

        # compute an approximate t_link [s]
        t_link = self.length / 200000

        if self.connection is not None:
            # tell the other node the starting time
            start_time = math.ceil(ns.sim_time() + t_link * (10 ** 9))
            # round up to a few nanoseconds before next clock cycle
            start_time = start_time + (self.t_clock - start_time % self.t_clock) + self.t_clock - 1
            print(f"[{ns.sim_time()}] Repeater {self.node.ID}: Sending START message with value {start_time}")
            self.node.ports["c0"].tx_output(ns.components.Message(items=["START", start_time]))

        else:
            # wait for the other node to tell the starting time
            print(f"[{ns.sim_time()}] Repeater {self.node.ID}: Waiting for START message")
            # mi fermo ad aspettare il messaggio di start
            yield self.await_port_input(self.node.ports["c0"])
            msg = self.node.ports["c0"].rx_input().items
            print(f"[{ns.sim_time()}] Repeater {self.node.ID}: Received START message")

            assert msg[0] == "START"

            start_time = msg[1]

        # wait until the starting time
        yield self.await_timer(end_time=start_time)

        # compute the t_round time (with a safety margin)
        t_round = self.K_attempts*self.t_clock + 1000

        success_index = None

        print(f"[{ns.sim_time()}] Repeater {self.node.ID}: Starting entanglement generation")

        while True:
            # wait for a photon to arrive or for the round to end
            ev_expr = yield self.await_port_input(self.node.ports["q0"]) | self.await_timer(end_time=start_time+t_round)

            # check if a photon has arrived
            if ev_expr.first_term.value and success_index is None: # se ho già ricevuto un fotone devo smettere di ascoltare
                # receive the photon
                qubit = self.node.ports["q0"].rx_input().items[0]

                # store the photon in the quantum memory
                self.node.qmemory.put(qubit, positions=[self.mem_position])

                # compute the current attempt index --> questa dipende dal tempo di simulazione
                success_index = math.floor((ns.sim_time() - start_time) / self.t_clock)
                print(f"[{ns.sim_time()}] Repeater {self.node.ID}: Latched photon at attempt {success_index}")

            # in this other case, the round has ended
            if ev_expr.second_term.value:
                if success_index is None:
                    success_index = -1
                # trasmetto all'altro nodo l'index dell'attempt che ha avuto successo
                final_msg = ns.components.Message(items=["END", success_index])
                self.node.ports["c0"].tx_output(final_msg)
                # wait for the end message by the other node
                yield self.await_port_input(self.node.ports["c0"])
                recv_end_msg = self.node.ports["c0"].rx_input().items
                assert recv_end_msg[0] == "END"

                # check if the success index is the same
                if recv_end_msg[1] != -1 and recv_end_msg[1] == success_index:
                    print(f"[{ns.sim_time()}] Repeater {self.node.ID}: Entanglement generation successful"
                          f" at attempt {success_index}")
                    # signal the success
                    self.send_signal(self.ENTANGLED_SIGNAL, result=None)

                    # the protocol can end
                    if self.connection is not None:
                        self.connection.subcomponents["EPS"].status = ns.components.qsource.SourceStatus.OFF
                    return

                # otherwise, the round has failed. start a new round
                else:
                    print(f"[{ns.sim_time()}] Repeater {self.node.ID}: Entanglement generation failed."
                          f" Starting new round")
                    start_time = ns.sim_time()
                    success_index = None
                    # devo liberare la memoria precedentemente occupata
                    if self.mem_position in self.node.qmemory.used_positions:
                        self.node.qmemory.pop(positions=self.mem_position)
