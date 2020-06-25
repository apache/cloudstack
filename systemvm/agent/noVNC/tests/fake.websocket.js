import Base64 from '../core/base64.js';

// PhantomJS can't create Event objects directly, so we need to use this
function make_event(name, props) {
    const evt = document.createEvent('Event');
    evt.initEvent(name, true, true);
    if (props) {
        for (let prop in props) {
            evt[prop] = props[prop];
        }
    }
    return evt;
}

export default class FakeWebSocket {
    constructor(uri, protocols) {
        this.url = uri;
        this.binaryType = "arraybuffer";
        this.extensions = "";

        if (!protocols || typeof protocols === 'string') {
            this.protocol = protocols;
        } else {
            this.protocol = protocols[0];
        }

        this._send_queue = new Uint8Array(20000);

        this.readyState = FakeWebSocket.CONNECTING;
        this.bufferedAmount = 0;

        this.__is_fake = true;
    }

    close(code, reason) {
        this.readyState = FakeWebSocket.CLOSED;
        if (this.onclose) {
            this.onclose(make_event("close", { 'code': code, 'reason': reason, 'wasClean': true }));
        }
    }

    send(data) {
        if (this.protocol == 'base64') {
            data = Base64.decode(data);
        } else {
            data = new Uint8Array(data);
        }
        this._send_queue.set(data, this.bufferedAmount);
        this.bufferedAmount += data.length;
    }

    _get_sent_data() {
        const res = new Uint8Array(this._send_queue.buffer, 0, this.bufferedAmount);
        this.bufferedAmount = 0;
        return res;
    }

    _open() {
        this.readyState = FakeWebSocket.OPEN;
        if (this.onopen) {
            this.onopen(make_event('open'));
        }
    }

    _receive_data(data) {
        // Break apart the data to expose bugs where we assume data is
        // neatly packaged
        for (let i = 0;i < data.length;i++) {
            let buf = data.subarray(i, i+1);
            this.onmessage(make_event("message", { 'data': buf }));
        }
    }
}

FakeWebSocket.OPEN = WebSocket.OPEN;
FakeWebSocket.CONNECTING = WebSocket.CONNECTING;
FakeWebSocket.CLOSING = WebSocket.CLOSING;
FakeWebSocket.CLOSED = WebSocket.CLOSED;

FakeWebSocket.__is_fake = true;

FakeWebSocket.replace = () => {
    if (!WebSocket.__is_fake) {
        const real_version = WebSocket;
        // eslint-disable-next-line no-global-assign
        WebSocket = FakeWebSocket;
        FakeWebSocket.__real_version = real_version;
    }
};

FakeWebSocket.restore = () => {
    if (WebSocket.__is_fake) {
        // eslint-disable-next-line no-global-assign
        WebSocket = WebSocket.__real_version;
    }
};
