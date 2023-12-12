package com.emranul.wirguardtest

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.emranul.wirguardtest.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import com.wireguard.config.InetEndpoint
import com.wireguard.config.InetNetwork
import com.wireguard.config.Interface
import com.wireguard.config.Peer
import com.wireguard.crypto.Key
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var tunnel: WgTunnel
    private lateinit var interfaceBuilder: Interface.Builder
    private lateinit var peerBuilder: Peer.Builder
    private lateinit var backend: GoBackend
    private var vpnConnected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.btnConnect.setOnClickListener {
            if (vpnConnected) {
                disconnectWireguard()
            } else {
                connectWireguard()
            }

        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun connectWireguard() {

       //under interface
        val address = "192.168.6.234/32"
        val privateKey = "CK5z3DhAq+5/TqcFf6/iMU056Kpvxktwr9CVgl5AT1w="
        val dnsList = listOf(
            "1.1.1.1",
            "8.8.8.8"
        )

        //under peer
        val allowedIp = listOf(
            "0.0.0.0/0",
            "::/0"
        )
        val publicKey = "cKLJBM2MuGrQncUVwnDd32Sqk4nGnoZhJ71MIMrFc2g="
        val endPoint = "be1.vpnjantit.com:1024"
        val persistentKeepAlive:String? = null





        interfaceBuilder = Interface.Builder()
        peerBuilder = Peer.Builder()
        backend = GoBackend(this)
        tunnel = WgTunnel()

        val configListOfDns = arrayListOf<InetAddress>()
        dnsList.forEach {
            configListOfDns.add(InetAddress.getByName(it))
        }

        val configListOfAllowIp = arrayListOf<InetNetwork>()

        allowedIp.forEach {
            configListOfAllowIp.add(InetNetwork.parse(it))
        }
        persistentKeepAlive?.let {
            peerBuilder.parsePersistentKeepalive(it)
        }

        peerBuilder.setPreSharedKey(Key.fromBase64(
            "bnEXsIeW0Vv/1hFwjqxreEKzwshSuRMh9HJxR+RbtFs="
        ))
        tunnel.stateChange = { state ->
            wireGuardTunnelStatus(state)
        }
        val intentPrepare = GoBackend.VpnService.prepare(this)
        if (intentPrepare != null) {
            registerForActivityResult.launch(intentPrepare)
        } else {
            try {
                GlobalScope.launch(coroutineExceptionHandler) {

                    backend.setState(
                        tunnel, Tunnel.State.UP, Config.Builder().setInterface(
                            interfaceBuilder.addAddress(
                                InetNetwork.parse(address)
                            ) //"20.9.0.47/16"
                                .addDnsServers(configListOfDns)
                                .parsePrivateKey(
                                    privateKey
                                )//eOgB74dGGtUM5brIBpFZnYqW7Q+g20xiLbBHUc3X1Xo=
                                .build()
                        )

                            .addPeer(
                                peerBuilder.addAllowedIps(
                                    configListOfAllowIp
                                ).setEndpoint(
                                    InetEndpoint.parse(
                                        endPoint
                                    )
                                )  //"sa-wg1.sshstores.net:2600"
                                    .parsePublicKey(publicKey)
                                    .build()
                            )  //"cPr5cwqVn6rFctOX1g+WRMxAeQDm0ImjDU6OESyeqlk="
                            .build()
                    )
                }


            } catch (e: Exception) {

                e.printStackTrace()

            }


        }

    }

    companion object {
        private const val TAG = "MainActivity"
    }

    private fun disconnectWireguard() {
        if (vpnConnected) {
            MaterialAlertDialogBuilder(this).apply {
                setTitle("Want to disconnect VPN?")
                setPositiveButton(
                    "Yes"
                ) { dialog, _ ->
                    dialog.dismiss()
                    backend.setState(tunnel, Tunnel.State.DOWN, null)
                }
                setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
                show()
            }

        }

    }

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        throwable.printStackTrace()
    }


    private val registerForActivityResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                connectWireguard()
            } else {
                Toast.makeText(
                    this,
                    "You have to give permission for connect VPN",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    private fun wireGuardTunnelStatus(state: Tunnel.State) {
        when (state) {
            Tunnel.State.DOWN -> {
                vpnConnected = false
                binding.apply {
                    btnConnect.text = "Connect"
                    textStatus.text = "Connection Down Re Connect"
                }

            }

            Tunnel.State.TOGGLE -> {
                vpnConnected = false
                binding.apply {
                    btnConnect.text = "Connecting.."
                    textStatus.text = "Loading..........."
                }
            }

            Tunnel.State.UP -> {
                vpnConnected = true
                binding.apply {
                    btnConnect.text = "Disconnect"
                    textStatus.text = "Connected Successfully"
                }
            }
        }
    }

}