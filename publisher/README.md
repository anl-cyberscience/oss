pubClient.py README

System Requirements:  python 2.7, httplib2 package, MySQLdb package

This file is designed to be used with a hub-spoke model TAXII messaging hub.
It provides users with access to the operations that a publisher would need in this messaging paradigm through a comand line interface.
Users should edit the associated config.py file in this directory prior to running pubClient.py.
Once properly configured, just navigate to this directory in the command line and execute the following:

$ python pubClient.py

The client should display a prompt with three options:  

(1) Create a new TAXII feed  
(2) Publish a TAXII Inbox Message  
(3) Exit

Users can navigate through this prompt menu to create new feeds and/or publish messages to the hub.

Please send questions to mwalters@bcmcgroup.com
