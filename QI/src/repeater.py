import netsquid as ns

# 2 - definisco un quantum repeater --> il repeater ha due porte: una per il quantum channel e una per il classical channel
class Repeater(ns.nodes.Node):
    """
    This class implements a quantum repeater node
    """

    def __init__(self, ID, name):
        """
        Initialize a Repeater.
        """
        super().__init__(name=name, ID=ID, port_names=["q0", "c0"])
        # self.qmemory = ns.components.QuantumMemory("qmemory", num_positions=2)

        # set di istruzioni supportate dal quantum processor
        physical_instructions = [
            # CNOT
            ns.components.PhysicalInstruction(ns.components.INSTR_CX, duration=1., parallel=True),
            # measure
            ns.components.PhysicalInstruction(ns.components.INSTR_MEASURE, duration=1., parallel=True)
        ]
        # ogni quantum repeater deve avere 4 memory slot
        qproc = ns.components.QuantumProcessor("qproc", num_positions=4, phys_instructions=physical_instructions)

        self.qmemory = qproc
