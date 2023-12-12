package com.emranul.wirguardtest

import android.util.Log
import com.wireguard.android.backend.Tunnel

class WgTunnel() : Tunnel {
    override fun getName(): String {
        return "wgpreconf"
    }

    override fun onStateChange(newState: Tunnel.State) {
        Log.d("VPN_CONNECTION", "onStateChange: $newState")
        stateChange?.invoke(newState)
    }

    var stateChange:((Tunnel.State)->Unit)? = null
}