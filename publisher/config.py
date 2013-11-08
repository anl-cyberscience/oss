#author - Mark Walters (mwalters@bcmcgroup.com)


DB_HOST = '127.0.0.1' # MySQL host
DB_USER = '' # MySQL user
DB_PASSWORD = '' # MySQL password
DB_DB = '' # MySQL database schema name
DB_PORT = 3306 # MySQL port

LAYER7_URL = 'https://layer7urlGoesHere:portGoesHere/endpoint/goes/here' # PEP URL
KEY = 'publisher.key' # User private key (PEM file)
CERT = 'publisher.crt' # User certificate (PEM file)

# TAXII Settings
CONTENT_BINDING = 'STIX_XML_1.0'
MESSAGE_BINDING = 'urn:taxii.mitre.org:message:xml:1.0'
PROTOCOL_BINDING = 'urn:taxii.mitre.org:protocol:https:1.0'
SERVICES_BINDING = 'urn:taxii.mitre.org:services:1.0'


import xml.etree.ElementTree as ET

# STIX Settings
ET.register_namespace('stix', 'http://stix.mitre.org/stix-1')
ET.register_namespace('indicator', 'http://stix.mitre.org/Indicator-2')
ET.register_namespace('cybox', 'http://cybox.mitre.org/cybox-2')
ET.register_namespace('URIObject', 'http://cybox.mitre.org/objects#URIObject-2')
ET.register_namespace('cyboxVocabs', 'http://cybox.mitre.org/default_vocabularies-2')
ET.register_namespace('stixVocabs', 'http://stix.mitre.org/default_vocabularies-1')
ET.register_namespace('stix', 'http://stix.mitre.org/stix-1')
ET.register_namespace('example', 'http://example.com/')
ET.register_namespace('testMechSnort', 'http://stix.mitre.org/extensions/TestMechanism#Snort-1')
ET.register_namespace('FileObj', 'http://cybox.mitre.org/objects#FileObject-2')
ET.register_namespace('cyboxCommon', 'http://cybox.mitre.org/common-2')
