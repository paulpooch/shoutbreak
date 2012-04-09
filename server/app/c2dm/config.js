/*
 * Required variables
 *
 * username (Google account ID)
 * password (Google account password)
 * source (Google source, like 'com-your-app')
 * port (port to bind to)
 * address (address to bind to)
 * debugServerPort (port to bind for stats / debug server)
 * debugServeraddress (address to bind for stats / debug server)
 * serverCallbackHost / serverCallbackPort / serverCallbackPath / serverCallbackSharedSecret /
 * serverCallbackProtocol
 * (if specified, will be used to send a POST back to a service in order to handle bad tokens)
 *
 */

module.exports = {
	username: 'shoutbreakcloud@gmail.com',
    password: 'echogolf7r33f0r7=^',
    source: 'co.shoutbreak',
    port: 8000,
    address: '127.0.0.1',
    debugServerPort: 8001,
    debugServeraddress: '127.0.0.2'
};
