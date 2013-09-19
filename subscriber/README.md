subServer.py and subClient.py README

System Requirements: python 2.7, httplib2 package, MySQLdb package

This file is designed to be used with a hub-spoke model TAXII messaging hub. 
It provides users with access to the operations that a subscriber would need in this messaging paradigm through a comand line interface. 
Users should edit the associated config.py file in this directory prior to running subServer.py or subClient.py. 

* Subscriber Listening Server  
Once properly configured, just navigate to this directory in the command line and execute the following:

$ python subServer.py <LISTENING_PORT>

The server will start up and give a notification that is running.  Use CTRL-C to kill it.


* Subscriber Client
Once properly configured, just navigate to this directory in the command line and execute the following:

$ python subClient.py <CALLBACK_URL>

The client should display a prompt with 10 options:

(1) Make a discovery request  
(2) Make a feed information request  
(3) List your current subscriptions  
(4) Subscribe to a feed  
(5) Unsubscribe from a feed  
(6) Pause a subscription  
(7) Resume a subscription  
(8) Request the status of a feed  
(9) Poll a subscription or feed  
(10) Exit  

Users can navigate through this prompt menu to perform any subscriber client interaction with the hub.

Note: Pause/Resume are not features of TAXII 1.0 but plan to be added to the next release of TAXII, we recognize that for now this is an extension of TAXII's capabilities.

Please send questions to mwalters@bcmcgroup.com
