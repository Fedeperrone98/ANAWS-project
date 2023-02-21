import math

import netsquid as ns
from repeater import Repeater
from midpoint_source import get_EPS_connection
from purification import PurificationProtocol


# 4 - creo la rete con tutti i componenti definiti fin adesso
def get_network(link_length, p_lr, p_m, t_clock):
    """
    This function returns a netsquid network having two quantum repeater nodes connected by an EPS Connection
    and a classical connection.

    Parameters
    ----------
    link_length : float
        The total length of the link between the repeaters in km.
    p_lr : float
        The loss probability of the left and right branches.
    p_m : float
        The probability of emitting the |00> state at each t_clock.
    t_clock : float
        The EPS clock period in nanoseconds.

    Returns
    -------
    network : netsquid.nodes.Network
        The network.
    """
    # create the network
    net = ns.nodes.Network("Quantum Repeater Network")

    # create the repeaters
    repeater1 = Repeater(ID=1, name="Repeater_1")
    repeater2 = Repeater(ID=2, name="Repeater_2")

    # add the repeaters and connections to the network
    net.add_node(repeater1)
    net.add_node(repeater2)

    # create the classical connection
    channel_A_to_B = ns.components.ClassicalChannel("channel_A_to_B", length=link_length,
                                                    models={"delay_model": ns.components.models.FibreDelayModel()})
    channel_B_to_A = ns.components.ClassicalChannel("channel_B_to_A", length=link_length,
                                                    models={"delay_model": ns.components.models.FibreDelayModel()})
    classical_conn = ns.nodes.DirectConnection("classical_conn", channel_A_to_B, channel_B_to_A)

    net.add_connection(node1=repeater1, node2=repeater2, connection=classical_conn,
                       port_name_node1="c0", port_name_node2="c0", label="classical_conn")

    # create the EPS connection
    eps_conn = get_EPS_connection(p_m=p_m, p_lr=p_lr, t_clock=t_clock, length=link_length)
    net.add_connection(node1=repeater1, node2=repeater2, connection=eps_conn,
                       port_name_node1="q0", port_name_node2="q0", label="eps_conn")

    K_attempts = math.ceil(1/(p_m*p_lr))

    purif_protocol1 = PurificationProtocol(node=repeater1, name="PP1", K_attempts=K_attempts, t_clock=t_clock,
                                           link_length=link_length, connection=eps_conn)
    purif_protocol2 = PurificationProtocol(node=repeater2, name="PP2", K_attempts=K_attempts, t_clock=t_clock,
                                           link_length=link_length, connection=None)
    purif_protocol1.start()
    purif_protocol2.start()

    return net

# 5 - creo il main
# Press the green button in the gutter to run the script.
if __name__ == '__main__':
    # rappresento i qubit come density matrix
    ns.set_qstate_formalism(ns.QFormalism.DM)
    network = get_network(link_length=30, p_lr=0.9, p_m=0.02, t_clock=10)
    ns.sim_run(end_time=10000000) # inizio la simulazione