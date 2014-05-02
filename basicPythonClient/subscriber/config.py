#author - Mark Walters (mwalters@bcmcgroup.com)


DB_HOST = '127.0.0.1' # MySQL host
DB_USER = '' # MySQL user
DB_PASSWORD = '' # MySQL password
DB_DB = '' # MySQL database schema name
DB_PORT = 3306 # MySQL port

LAYER7_URL = 'https://layer7urlGoesHere:portGoesHere/endpoint/goes/here' # PEP URL
KEY = 'subscriber.key' # User private key (PEM file)
CERT = 'subscriber.crt' # User certificate (PEM file)
CACERTS = '/path/to/trusted/certs/file' # Ensure that this PEM file has certificate of PEP in it

# TAXII Settings
PROTOCOL_BINDING = 'urn:taxii.mitre.org:protocol:https:1.0'
MESSAGE_BINDING = 'urn:taxii.mitre.org:message:xml:1.0'
SERVICES = 'urn:taxii.mitre.org:services:1.0'
CONTENT_BINDING = 'STIX_XML_1.0'
