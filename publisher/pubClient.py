from httplib2 import Http
import sys
from uuid import uuid1
import xml.etree.ElementTree as ET
import os
import MySQLdb
import config
import time

HEADERS = {
    'Accept': 'application/xml',
    'Content-Type': 'application/xml', 
    'User-Agent': 'TAXII client publisher application',
    'X-TAXII-Accept': config.MESSAGE_BINDING,
    'X-TAXII-Content-Type': config.MESSAGE_BINDING,
    'X-TAXII-Protocol': config.PROTOCOL_BINDING,
    'X-TAXII-Services': config.SERVICES_BINDING
    }


class InboxMessage:
    def __init__(self, cBlocks):
        self.message_id = str(uuid1())
        self.message = ''
        self.content_blocks = cBlocks

    def generateXML(self):
        root_attr = {'message_id': self.message_id}
        root = ET.Element('Inbox_Message', root_attr)
        if self.message:
            msg = ET.SubElement(root, 'Message')
            msg.text = self.message
        for cBlock in self.content_blocks:
            content_block = ET.SubElement(root, 'Content_Block')
            cB = ET.SubElement(content_block, 'Content_Binding')
            cB.text = cBlock['Content_Binding']
            content = ET.SubElement(content_block, 'Content')
            content.append(cBlock['Content'])
            if cBlock.get('Timestamp_Label'):
                tS = ET.SubElement(content_block, 'Timestamp_Label')
                tS.text = cBlock['Timestamp_Label']
        print 'Request XML: '+ET.tostring(root)+'\n'
        return ET.tostring(root)

def createFeed():
    try:
        conn = MySQLdb.connect(host=config.DB_HOST, port=config.DB_PORT, user=config.DB_USER, passwd=config.DB_PASSWORD, db=config.DB_DB)
        cursor = conn.cursor()
    except Exception as e: 
        print 'Error connecting to MySQL: {0}.  Create new feed failed!'.format(e)
        return
    try:
        fN = raw_input("Feed Name:  ")
        while not fN: fN = raw_input('Feed Name cannot be empty.  Enter Feed Name:  ')
        desc = raw_input('Description:  ')
        while not desc: desc = raw_input('Description cannot be empty.  Enter Description:  ')
        cursor.execute("INSERT INTO topicTree (id, deliveryFormat, feedName, description) VALUES ('{0}', 'TAXII', '{1}', '{2}')".format(str(uuid1()), fN.replace("'", "\\'"), desc.replace("'", "\\'")))
        cursor.execute('COMMIT')
    except Exception as e:
        print 'Create new feed failed: {0}'.format(e)
        return
    finally:
        cursor.close()
        conn.close()

def createSubFeed():
    try:
        conn = MySQLdb.connect(host=config.DB_HOST, port=config.DB_PORT, user=config.DB_USER, passwd=config.DB_PASSWORD, db=config.DB_DB)
        cursor = conn.cursor()
    except Exception as e: 
        print 'Error connecting to MySQL: {0}.  Create new feed failed!'.format(e)
        return
    try:
        cursor.execute("SELECT id, feedName, description FROM topicTree WHERE deliveryFormat='TAXII'")
        feedList = cursor.fetchall()
        print 'Here are the current available feeds:\n'
        for i,feed in enumerate(feedList): print '\t({0}) {1}\t{2}'.format(i+1, feed[1], feed[2])
        print '\t(B) GO BACK'
        choice = 0
        while choice == 0:
            choice = raw_input('Select a feed from the list:  ')
            if choice.lower() == 'b': return
            elif not isinstance(choice, int) or choice not in range(1,len(feedList)+1): 
                print 'Invalid entry, please try again.'
                choice = 0
        parentFeed = feedList[choice-1]
        parentID = parentFeed[0]
        parentName = parentFeed[1]
        print '\nSub-feed of {0}'.format(parentName)
        fN = raw_input('Feed Name:  ')
        while not fN: fN = raw_input('Feed Name cannot be empty.  Enter Feed Name:  ')
        desc = raw_input('Description:  ')
        while not desc: desc = raw_input('Description cannot be empty.  Enter Description:  ')
        cursor.execute("INSERT INTO topicTree (id, parent_id, deliveryFormat, feedName, description) VALUES ('{0}', '{1}', 'TAXII', '{2}', '{3}')".format(str(uuid1()), parentID, fN, desc))
        cursor.execute('COMMIT')
    except Exception as e: 
        print 'Create new feed failed: {0}'.format(e)
        return 
    finally:
        cursor.close()
        conn.close()

def createPrompt():
    os.system('cls' if os.name=='nt' else 'clear')
    print '\n--------------------'
    print 'Create New Feed Menu'
    print '--------------------\n'
    sF = raw_input('Is this a sub-feed of an existing feed? (Y or [N]):  ')
    if not sF or sF[0].lower() == 'n': createFeed()
    elif sF[0].lower() == 'y': createSubFeed()
    else: return
    raw_input('Press any key to return to main menu.')


def prompt():
    print '\n----------------------------------'
    print 'TAXII Publish Client Menu Prompt'
    print '----------------------------------\n'
    print 'What would you like to do?'
    print '(1) Create a new TAXII feed'
    print '(2) Publish a TAXII Inbox message'
    print '(3) Exit\n'

def publish(fN, content_blocks, message=None):
    req = InboxMessage(content_blocks)
    if message: req.message = message
    xml = req.generateXML()
    HEADERS['Content-Length'] = str(len(xml))
    conn = Http()
    conn.add_certificate(config.KEY, config.CERT, '')
    responseHeaders, responseBody = conn.request(config.LAYER7_URL+'/inbox/'+fN, 'POST', body=xml, headers=HEADERS)
    print 'Response XML: '+responseBody+'\n'

def publishPrompt():
    os.system('cls' if os.name=='nt' else 'clear')
    print '\n------------'
    print 'Publish Menu'
    print '------------\n'
    fN = raw_input("Feed Name:  ")
    while not fN: fN = raw_input("Feed Name:  ")
    cnt, ans = 1, 'y'
    content_blocks = []
    while ans == 'y':
        print '\nContent Block #{0}...'.format(cnt)
        cB = raw_input("Content Binding (Default '{0}'):  ".format(config.CONTENT_BINDING))
        if not cB: cB = config.CONTENT_BINDING
        content = raw_input('Enter XML file:  ')
        try: content = ET.parse(content).getroot()
        except Exception as e:
            print 'Error ingesting XML file: {0}.  Quitting now!'.format(e)
            sys.exit(1)
        tS = raw_input("Timestamp Label (Default '{0}'):  ".format(time.strftime('%Y-%m-%dT%H:%M:%SZ', time.localtime())))
        if not tS: tS = time.strftime('%Y-%m-%dT%H:%M:%SZ', time.localtime())
        content_blocks.append({'Content_Binding': cB, 'Content': content, 'Timestamp_Label': tS})
        cnt += 1
        ans = raw_input("Enter another content block? ('y' or 'n')?  ").strip().lower()
    publish(fN, content_blocks)
    raw_input('Press any key to return to main menu.')

# prompt loop
choice = 0
while choice != 2:
    os.system('cls' if os.name=='nt' else 'clear')
    prompt()
    while choice not in range(1,4): 
        try: choice = int(raw_input('Enter selection: '))
        except: pass
    if choice == 1: createPrompt()
    if choice == 2: publishPrompt()
    elif choice == 3: sys.exit(0)
    choice = 0
