/*
 *  Copyright (C) 2013-2016 Antony Hornacek (magicbox@imejl.sk)
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package bruenor.magicbox;

import android.content.Context;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import magiclib.controls.Dialog;
import magiclib.dosbox.SerialPort;
import magiclib.locales.Localization;
import magiclib.logging.MessageInfo;

class IPXSettings extends Dialog
{
    public abstract interface OnIPXEditEventListener
    {
        public abstract void onSave(boolean enabled, boolean ask, boolean clientOn, int serverPort, String clientToIp, int clientToPort);
    }

    public abstract interface OnIPXStartEventListener
    {
        public abstract void onSave(boolean enabled, boolean isChange, boolean doCancel, boolean ask, boolean clientOn, int serverPort, String clientToIp, int clientToPort);
    }

    private CheckBox ipxEnabled;
    private CheckBox ipxAsk;
    private CheckBox useServer;
    private CheckBox useClient;
    private EditText serverPort;
    private EditText clientIP;
    private EditText clientPort;
    private boolean inCheck = false;
    private OnIPXEditEventListener editEvent;
    private OnIPXStartEventListener startEvent;

    private boolean originEnabled;
    private boolean originAskAtStart;
    private boolean originClientOn;
    private int originServerPort;
    private String originClientToIP;
    private int originClientToPort;

    private boolean newClientOn;
    private int newServerPort;
    private String newClientToIP;
    private int newClientToPort;

    private boolean changed = false;
    private boolean doCancel = true;

    @Override
    public void onSetLocalizedLayout()
    {
        localize(R.id.ipx_on, "common_enabled");
        localize(R.id.ipx_server_on, "ipx_start_server");
        localize(R.id.ipx_server_port_title, "common_port");
        localize(R.id.ipx_client_on, "ipx_connect_client");
        localize(R.id.ipx_client_ip_title, "common_ip");
        localize(R.id.ipx_client_port_title, "common_port");
        localize(R.id.ipx_client_title, "common_client");
        localize(R.id.ipx_ask, "ipx_ask");
    }

    public IPXSettings(boolean started, boolean enabled, boolean ipxAskAtStart, boolean clientOn, int serverPort, String clientToIP, int clientToPort) {
        super(AppGlobal.context);

        setContentView(R.layout.ipx);
        setCaption("ipx_title");

        originEnabled = enabled;

        ipxEnabled = (CheckBox)findViewById(R.id.ipx_on);
        ipxEnabled.setChecked(enabled);

        ipxAsk = (CheckBox)findViewById(R.id.ipx_ask);
        ipxAsk.setChecked(ipxAskAtStart);

        originAskAtStart = ipxAskAtStart;
        originClientOn = clientOn;
        originServerPort = serverPort;
        originClientToIP = clientToIP;
        originClientToPort = clientToPort;

        CompoundButton.OnCheckedChangeListener onCheck = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                if (inCheck)
                    return;

                inCheck = true;

                switch (buttonView.getId())
                {
                    case R.id.ipx_server_on: {
                        useServer.setChecked(true);
                        useClient.setChecked(false);
                        break;
                    }
                    case R.id.ipx_client_on: {
                        useServer.setChecked(false);
                        useClient.setChecked(true);
                        break;
                    }
                }

                inCheck = false;
            }
        };

        useServer = (CheckBox)findViewById(R.id.ipx_server_on);
        useServer.setChecked(!clientOn);
        useServer.setOnCheckedChangeListener(onCheck);

        useClient = (CheckBox)findViewById(R.id.ipx_client_on);
        useClient.setChecked(clientOn);
        useClient.setOnCheckedChangeListener(onCheck);

        this.serverPort = (EditText)findViewById(R.id.ipx_server_port);
        this.serverPort.setText("" + serverPort);

        this.clientIP = (EditText)findViewById(R.id.ipx_client_ip);
        this.clientIP.setText(clientToIP);

        this.clientPort = (EditText)findViewById(R.id.ipx_client_port);
        this.clientPort.setText("" + clientToPort);

        TextView serverTitle = (TextView)findViewById(R.id.ipx_server_title);
        serverTitle.setText(Localization.getString("common_server") + " (" + AppGlobal.getIPAddress(true) + ")");

        ImageButton confirm = (ImageButton)findViewById(R.id.ipx_confirm);
        confirm.setOnClickListener(confirmEvent());
    }

    @Override
    protected void onStop()
    {
        super.onStop();

        if (editEvent != null) {
            editEvent.onSave(ipxEnabled.isChecked(), ipxAsk.isChecked(), newClientOn, newServerPort, newClientToIP, newClientToPort);
        } else {
            startEvent.onSave(ipxEnabled.isChecked(), changed, doCancel, ipxAsk.isChecked(), newClientOn, newServerPort, newClientToIP, newClientToPort);
        }

    }

    private View.OnClickListener confirmEvent()
    {
        return new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                String port = serverPort.getText().toString();
                if (!port.matches("^[1-9]\\d*$")) {
                    MessageInfo.info("msg_port_format");
                    return;
                }

                int serverPort = Integer.parseInt(port);

                port = clientPort.getText().toString();
                if (!port.matches("^[1-9]\\d*$")) {
                    MessageInfo.info("msg_port_format");
                    return;
                }

                int clientPort = Integer.parseInt(port);

                String IP = clientIP.getText().toString();
                if (!IP.matches("^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$")) {
                    MessageInfo.info("msg_ip_format");
                    return;
                }

                newClientOn = useClient.isChecked();
                newServerPort = serverPort;
                newClientToIP = IP;
                newClientToPort = clientPort;

                if (startEvent != null) {
                    changed = !((originAskAtStart == ipxAsk.isChecked()) &&
                            (newClientOn == originClientOn) &&
                            (newServerPort == originServerPort) &&
                            (newClientToIP.equals(originClientToIP)) &&
                            (newClientToPort == originClientToPort) &&
                            (originEnabled == ipxEnabled.isChecked()));
                    doCancel = false;
                }

                dismiss();
            }
        };
    }

    public void setOnIPXEditEventListener(OnIPXEditEventListener event)
    {
        this.editEvent = event;
    }

    public void setOnIPXStartEventListener(OnIPXStartEventListener event)
    {
        this.startEvent = event;
    }
}

class SerialPortSettings extends Dialog
{
    public abstract interface OnSerialPortEventListener
    {
        public abstract void onSave(SerialPort serial, int port);
    }

    private OnSerialPortEventListener event;
    private CheckBox deviceEnabled;
    private EditText devicePort;

    @Override
    public void onSetLocalizedLayout()
    {
        localize(R.id.serialport_serial_enabled, "common_enabled");
        localize(R.id.serialport_port_title, "common_port");
    }

    public SerialPortSettings(Context context, SerialPort serial, int port) {
        super(context);

        setContentView(R.layout.serialport);
        setCaption("serial_title");

        deviceEnabled = (CheckBox)findViewById(R.id.serialport_serial_enabled);
        deviceEnabled.setChecked(serial == SerialPort.modem);

        devicePort = (EditText)findViewById(R.id.serialport_port);
        devicePort.setText(port>0?""+port:"");

        ImageButton confirm = (ImageButton)findViewById(R.id.serialport_confirm);
        confirm.setOnClickListener(confirmEvent());
    }

    private View.OnClickListener confirmEvent()
    {
        return new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (event != null) {

                    String port = devicePort.getText().toString().trim();
                    int modemPort;

                    if (port.equals("")) {
                        modemPort = 0;
                    } else {
                        if (!port.matches("^[1-9]\\d*$")) {
                            return;
                        }

                        modemPort = Integer.parseInt(port);
                    }

                    event.onSave(deviceEnabled.isChecked()?SerialPort.modem:SerialPort.disabled, modemPort);
                }

                dismiss();
            }
        };
    }

    public void setOnSerialPortEventListener(OnSerialPortEventListener event) {
        this.event = event;
    }
}