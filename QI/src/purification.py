import netsquid as ns

from midpoint_source import MSProtocol


# 6 - implemento il purification protocol --> utilizza il MS protocol per generare entangled qubit pairs, quindi applica la purification per migliorare la fidelity
class PurificationProtocol(ns.protocols.NodeProtocol):

    def __init__(self, node, name=None, K_attempts=200, t_clock=10, link_length=25, connection=None):
        super().__init__(node=node, name=name)
        self.add_subprotocol(MSProtocol(self.node, K_attempts=K_attempts, link_length=link_length, t_clock=t_clock,
                                        connection=connection, mem_position=0), name="MSProtocol_0")
        self.add_subprotocol(MSProtocol(self.node, K_attempts=K_attempts, link_length=link_length, t_clock=t_clock,
                                        connection=connection, mem_position=1), name="MSProtocol_1")

    @staticmethod
    def _get_purification_program():
        # create a program that will be used to purify the qubits
        program = ns.components.QuantumProgram(num_qubits=2)
        q1, q2 = program.get_qubit_indices(2)
        program.apply(ns.components.INSTR_CX, qubit_indices=[q1, q2])
        program.apply(ns.components.INSTR_MEASURE, [q2], output_key="M0")
        return program

    def _get_fidelity(self, position):
        qubits = self.node.qmemory.peek(positions=[position])[0].qstate.qubits
        fidelity = ns.qubits.qubitapi.fidelity(qubits, ns.qubits.ketstates.b00, squared=True)
        return fidelity

    def run(self):
        # first thing to do is to create the entangled pair on the first memory slot
        self.subprotocols["MSProtocol_0"].start()

        # wait for the first MSProtocol to finish
        ev_expr = self.await_signal(sender=self.subprotocols["MSProtocol_0"], signal_label=MSProtocol.ENTANGLED_SIGNAL)
        yield ev_expr

        # start the second MSProtocol to entangle the second qubit
        self.subprotocols["MSProtocol_1"].start()

        # wait for the second MSProtocol to finish
        ev_expr = self.await_signal(sender=self.subprotocols["MSProtocol_1"], signal_label=MSProtocol.ENTANGLED_SIGNAL)
        yield ev_expr

        # get the fidelity of the qubits
        fidelity_0 = self._get_fidelity(0)
        fidelity_1 = self._get_fidelity(1)

        # print the fidelities
        print(f"[{ns.sim_time()}] Repeater {self.node.ID}: Both qubits are entangled with fidelity {fidelity_0} and {fidelity_1}")

        # at this point we have two entangled qubits in the memory
        prog = self._get_purification_program()
        self.node.qmemory.execute_program(prog, qubit_mapping=[0, 1], error_on_fail=True)
        yield self.await_program(self.node.qmemory)

        # we collect the measurement result
        outcome = prog.output["M0"][0]

        # we send the measurement result to the other node
        self.node.ports["c0"].tx_output(ns.components.Message(items=outcome))

        # we wait for the measurement result from the other node
        yield self.await_port_input(self.node.ports["c0"])
        msg = self.node.ports["c0"].rx_input()
        outcome_other = msg.items[0]

        # we check if the measurement results are the same
        if outcome == outcome_other:
            print(f"[{ns.sim_time()}] Purification successful")
            # print the new qubit fidelity with respect to the bell state
            print(f"[{ns.sim_time()}] Fidelity of the new qubit pair with respect to the Bell state: {self._get_fidelity(position=0)}")
        else:
            print(f"[{ns.sim_time()}] Purification failed")
            # discard the qubit from memory
            self.node.qmemory.pop(positions=0)
