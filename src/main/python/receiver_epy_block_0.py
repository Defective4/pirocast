import pmt
from gnuradio import gr


class blk(gr.sync_block):

    def __init__(self):
        gr.sync_block.__init__(
            self,
            name='Message to Command',
            in_sig=None,
            out_sig=None
        )

        self.message_port_register_in(pmt.intern("inpair"))
        self.set_msg_handler(pmt.intern("inpair"), self.msg_handler)

    def init_callbacks(self, demod, demod_freq, enable_rds, fm_stereo, deemp, gain, center_freq):
        self.callbacks = {
            "demod": demod,
            "demod_freq": demod_freq,
            "enable_rds": enable_rds,
            "fm_stereo": fm_stereo,
            "deemp": deemp,
            "gain": gain,
            "center_freq": center_freq
        }

    def msg_handler(self, msg):
        if not pmt.is_pair(msg) or pmt.is_dict(msg) or pmt.is_pdu(msg):
            gr.log.warn(
                "Input message %s is not a simple pair, dropping" % repr(msg))
            return

        val = pmt.to_python(pmt.cdr(msg))
        key = pmt.to_python(pmt.car(msg))
        self.callbacks[key](val)

    def stop(self):
        return True
