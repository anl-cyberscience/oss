from httplib2 import Http
import urllib
import sys
from uuid import uuid1
import xml.etree.ElementTree as ET
import MySQLdb
import os
import config

if len(sys.argv) != 2:
    print 'Usage: {0} <CALLBACK_URL>'.format(sys.argv[0])
    sys.exit(1)
else:
    CALLBACK = sys.argv[1]

HEADERS = {
    'Accept': 'application/xml',
    'Content-Type': 'application/xml', 
    'User-Agent': 'TAXII client subscriber application',
    'X-TAXII-Accept': config.MESSAGE_BINDING,
    'X-TAXII-Content-Type': config.MESSAGE_BINDING,
    'X-TAXII-Protocol': config.PROTOCOL_BINDING,
    'X-TAXII-Services': config.SERVICES
}

# fetch user email
USER_EMAIL = None
while not USER_EMAIL: USER_EMAIL = raw_input("Please enter user email: ")

# initialize subscriptions
subscriptions = dict() 
conn = MySQLdb.connect(host=config.DB_HOST, user=config.DB_USER, passwd=config.DB_PASSWORD, db=config.DB_DB, port=config.DB_PORT)
cursor = conn.cursor()
cursor.execute("SELECT id, feedName, protocolBinding, messageBinding, contentBinding, state FROM subscriptions WHERE (email='{0}' AND deliveryFormat='TAXII')".format(USER_EMAIL))
for subID,fN,pB,mB,cB,st in cursor.fetchall(): subscriptions[fN] = {'subscription_id': subID, 'Protocol_Binding': pB, 'Message_Binding': mB, 'Content_Binding': [cB], 'state': st}
cursor.close()
conn.close()



class SubscriptionManagementRequest:
    def __init__(self, fN, a):
        self.message_id = str(uuid1())
        self.action = a
        self.feed_name = fN
        if a in ['UNSUBSCRIBE', 'PAUSE', 'RESUME']: self.subscription_id = ''
        if a == 'SUBSCRIBE': 
            self.protocol_binding = self.address = self.message_binding = ''
            self.content_binding = []

    def generateXML(self):
        root_attr = {'message_id': self.message_id, 'action': self.action, 'feed_name': self.feed_name}
        if self.action in ['UNSUBSCRIBE', 'PAUSE', 'RESUME']: root_attr['subscription_id'] = self.subscription_id
        root = ET.Element('Subscription_Management_Request', root_attr)
        if self.action == 'SUBSCRIBE' and self.protocol_binding and self.address and self.message_binding:
            push_parameters = ET.SubElement(root, 'Push_Parameters')
            protocol_binding = ET.SubElement(push_parameters, 'Protocol_Binding')
            protocol_binding.text = self.protocol_binding
            address = ET.SubElement(push_parameters, 'Address')
            address.text = self.address
            message_binding = ET.SubElement(push_parameters, 'Message_Binding')
            message_binding.text = self.message_binding
            for cB in self.content_binding:
                content_binding = ET.SubElement(push_parameters, 'Content_Binding')
                content_binding.text = cB
        print 'Request XML: '+ET.tostring(root)+'\n'
        return ET.tostring(root)


def discoveryRequest():
    os.system('cls' if os.name=='nt' else 'clear')
    print '\n-----------------'
    print 'Discovery Request'
    print '-----------------\n'
    root_attr = {'message_id': str(uuid1())}
    root = ET.Element('Discovery_Request', root_attr)
    xml = ET.tostring(root)
    HEADERS['Content-Length'] = str(len(xml))
    conn = Http()
    conn.add_certificate(config.KEY, config.CERT, '')
    responseHeaders, responseBody = conn.request(config.LAYER7_URL+'/discovery', 'POST', body=xml, headers=HEADERS)
    print 'Response XML: '+responseBody+'\n'
    raw_input('Press any key to return to main menu.')


def feedInformationRequest():
    os.system('cls' if os.name=='nt' else 'clear')
    print '\n------------------------'
    print 'Feed Information Request'
    print '------------------------\n'
    root_attr = {'message_id': str(uuid1())}
    root = ET.Element('Feed_Information_Request', root_attr)
    xml = ET.tostring(root)
    HEADERS['Content-Length'] = str(len(xml))
    conn = Http()
    conn.add_certificate(config.KEY, config.CERT, '')
    responseHeaders, responseBody = conn.request(config.LAYER7_URL+'/feedinfo', 'POST', body=xml, headers=HEADERS)
    print 'Response XML: '+responseBody+'\n'
    raw_input('Press any key to return to main menu.')
 
def listSubscriptions():
    refreshSubscriptions()
    os.system('cls' if os.name=='nt' else 'clear')
    print '-'*86
    print '\tFeed Name\t\tSubscription ID\t\t\t\t\tStatus'
    print '-'*86
    for i,sub in enumerate(subscriptions): 
        if subscriptions[sub]['state'] == 'ACTIVE': print '{0}.\t{1}'.format(i+1, sub)+(3-len(sub)/8)*'\t'+'{0}\t\tACTIVE'.format(subscriptions[sub]['subscription_id'])
        elif subscriptions[sub]['state'] == 'PAUSED': print '{0}.\t{1}'.format(i+1, sub)+(3-len(sub)/8)*'\t'+'{0}\t\tPAUSED'.format(subscriptions[sub]['subscription_id'])
        else: print '{0}.\t{1}'.format(i+1, sub)+(3-len(sub)/8)*'\t'+'{0}\t\tPENDING'.format(subscriptions[sub]['subscription_id'])
    print '\n'
    raw_input('Press any key to return to main menu.')

def pause(fN, subID):
    req = SubscriptionManagementRequest(fN, 'PAUSE')
    req.subscription_id = subID
    subMgmtReq(fN, req)
    refreshSubscriptions()

def pausePrompt():
    os.system('cls' if os.name=='nt' else 'clear')
    print '\n----------'
    print 'Pause Menu'
    print '----------\n'
    fN = raw_input("Feed Name:  ")
    if fN not in subscriptions: 
        print 'Not subscribed to feed: {0}\n'.format(fN)
        raw_input('Press any key to return to main menu.')
        return
    subID = subscriptions[fN]['subscription_id']
    pause(fN, subID)
    raw_input('Press any key to return to main menu.')

def poll(fN, bT, eT, cB, subID):
    root_attr = {'message_id': str(uuid1()), 'feed_name': fN, 'subscription_id': subID}
    root = ET.Element('Poll_Request', root_attr)
    if bT: 
        beginTime = ET.SubElement(root, 'Exclusive_Begin_Timestamp')
        beginTime.text = bT
    if eT: 
        endTime = ET.SubElement(root, 'Inclusive_End_Timestamp')
        endTime.text = eT
    if cB:
        contentBinding = ET.SubElement(root, 'Content_Binding')
        contentBinding.text = cB
    print 'Request XML: '+ET.tostring(root)+'\n'
    xml = ET.tostring(root)
    HEADERS['Content-Length'] = str(len(xml))
    conn = Http()
    conn.add_certificate(config.KEY, config.CERT, '')
    responseHeaders, responseBody = conn.request(config.LAYER7_URL+'/poll', 'POST', body=xml, headers=HEADERS)
    print 'Response XML: '+responseBody+'\n'

def pollPrompt():
    os.system('cls' if os.name=='nt' else 'clear')
    print '\n---------'
    print 'Poll Menu'
    print '---------\n'
    fN = raw_input('Feed name: ')
    while not fN: raw_input('Feed name is required.  Enter feed name: ')
    choice = raw_input('Would you like to enter an exclusive begin timestamp? (Y or [N]): ')
    bT = eT = None
    if choice and choice[0].lower() == 'y':
        yyyy = raw_input('Four Digit Year: ')
        mm = raw_input('Two Digit Month: ')
        dd = raw_input('Two Digit Day: ')
        hh = raw_input('Two Digit Hour (00-24): ')
        mm = raw_input('Two Digit Minute: ')
        ss = raw_input('Two Digit Second: ')
        bT = yyyy+'-'+mm+'-'+dd+'T'+hh+':'+mm+':'+ss+'Z'
    choice = raw_input('Would you like to enter an inclusive end timestamp? (Y or [N]): ')
    if choice and choice[0].lower() == 'y':
        yyyy = raw_input('Four Digit Year: ')
        mm = raw_input('Two Digit Month: ')
        dd = raw_input('Two Digit Day: ')
        hh = raw_input('Two Digit Hour (00-24): ')
        mm = raw_input('Two Digit Minute: ')
        ss = raw_input('Two Digit Second: ')
        eT = yyyy+'-'+mm+'-'+dd+'T'+hh+':'+mm+':'+ss+'Z'
    cB = raw_input("Content binding: (Default '{0}')".format(config.CONTENT_BINDING))
    if not cB: cB = config.CONTENT_BINDING
    if fN in subscriptions: poll(fN, bT, eT, cB, subscriptions[fN]['subscription_id'])
    else: print "DENIED - You are not subscribed to the feed '{0}'".format(fN)
    raw_input('Press any key to return to main menu.')

def prompt():
    print '\n----------------------------------'
    print 'TAXII Subscribe Client Menu Prompt'
    print '----------------------------------\n'
    print 'What would you like to do?'
    print '(1) Make a discovery request'
    print '(2) Make a feed information request'
    print '(3) List your current subscriptions'
    print '(4) Subscribe to a feed'
    print '(5) Unsubscribe from a feed'
    print '(6) Pause a subscription'
    print '(7) Resume a subscription'
    print '(8) Request the status of a feed'
    print '(9) Poll a subscription or feed'
    print '(10) Exit\n'

def refreshSubscriptions():
    subscriptions.clear()
    conn = MySQLdb.connect(host=config.DB_HOST, user=config.DB_USER, passwd=config.DB_PASSWORD, db=config.DB_DB, port=config.DB_PORT)
    cursor = conn.cursor()
    cursor.execute("SELECT id, feedName, protocolBinding, messageBinding, contentBinding, state FROM subscriptions WHERE (email='{0}' AND deliveryFormat='TAXII')".format(USER_EMAIL))
    for subID,fN,pB,mB,cB,st in cursor.fetchall(): subscriptions[fN] = {'subscription_id': subID, 'Protocol_Binding': pB, 'Message_Binding': mB, 'Content_Binding': [cB], 'state': st}
    cursor.close()
    conn.close()

def resume(fN, subID):
    req = SubscriptionManagementRequest(fN, 'RESUME')
    req.subscription_id = subID
    subMgmtReq(fN, req)
    refreshSubscriptions()

def resumePrompt():
    os.system('cls' if os.name=='nt' else 'clear')
    print '\n-----------'
    print 'Resume Menu'
    print '-----------\n'
    fN = raw_input("Feed Name:  ")
    if fN not in subscriptions: 
        print 'Not subscribed to feed: {0}\n'.format(fN)
        raw_input('Press any key to return to main menu.')
        return
    subID = subscriptions[fN]['subscription_id']
    resume(fN, subID)
    raw_input('Press any key to return to main menu.')

def status(fN):
    req = SubscriptionManagementRequest(fN, 'STATUS')
    subMgmtReq(fN, req)

def statusPrompt():
    os.system('cls' if os.name=='nt' else 'clear')
    print '\n-----------'
    print 'Status Menu'
    print '-----------\n'
    fN = raw_input("Feed Name:  ")
    status(fN)
    raw_input('Press any key to return to main menu.')

def subMgmtReq(fN, req):
    xml = req.generateXML()
    HEADERS['Content-Length'] = str(len(xml))
    conn = Http()
    conn.add_certificate(config.KEY, config.CERT, '')
    responseHeaders, responseBody = conn.request(config.LAYER7_URL+'/subscription', 'POST', body=xml, headers=HEADERS)
    print 'Response XML: '+responseBody+'\n'

def subscribe(fN, pB='', addr='', mB='', cB=[]):
    req = SubscriptionManagementRequest(fN, 'SUBSCRIBE')
    req.protocol_binding = pB
    req.address = addr
    req.message_binding = mB
    req.content_binding.extend(cB) 
    subMgmtReq(fN, req)
    refreshSubscriptions()

def subscribePrompt():
    os.system('cls' if os.name=='nt' else 'clear')
    print '\n--------------'
    print 'Subscribe Menu'
    print '--------------\n'
    fN = raw_input("Feed Name:  ")
    while not fN: fN = raw_input("Feed Name:  ")
    ans = raw_input("Would you like to have content pushed to you? ([Y] or N):  ")
    if ans != 'N':
        pB = raw_input("Protocol Binding (Default '{0}'):  ".format(config.PROTOCOL_BINDING))
        if not pB: pB = config.PROTOCOL_BINDING
        addr = raw_input("Address (Default '{0}'):  ".format(CALLBACK))
        if not addr: addr = CALLBACK
        mB = raw_input("Message Binding (Default '{0}'):  ".format(config.MESSAGE_BINDING))
        if not mB: mB = config.MESSAGE_BINDING
        cB = raw_input("Content Binding (Default '{0}'):  ".format(config.CONTENT_BINDING))
        if cB: cB = cB.split()
        else: cB = [config.CONTENT_BINDING]
        subscribe(fN, pB, addr, mB, cB)
    else: subscribe(fN)
    raw_input('Press any key to return to main menu.')

def unsubscribe(fN, subID):
    req = SubscriptionManagementRequest(fN, 'UNSUBSCRIBE')
    req.subscription_id = subID
    subMgmtReq(fN, req)
    refreshSubscriptions()

def unsubscribePrompt():
    os.system('cls' if os.name=='nt' else 'clear')
    print '\n----------------'
    print 'Unsubscribe Menu'
    print '----------------\n'
    fN = raw_input("Feed Name:  ")
    if fN not in subscriptions: 
        print 'Not subscribed to feed: {0}\n'.format(fN)
        raw_input('Press any key to return to main menu.')
        return
    subID = subscriptions[fN]['subscription_id']
    unsubscribe(fN, subID)
    raw_input('Press any key to return to main menu.')


# prompt loop
choice = 0
while choice != 10:
    os.system('cls' if os.name=='nt' else 'clear')
    prompt()
    while choice not in range(1,11): 
        try: choice = int(raw_input('Enter selection: '))
        except: pass
    if choice == 1: discoveryRequest()
    elif choice == 2: feedInformationRequest()
    elif choice == 3: listSubscriptions()
    elif choice == 4: subscribePrompt()
    elif choice == 5: unsubscribePrompt()
    elif choice == 6: pausePrompt()
    elif choice == 7: resumePrompt()
    elif choice == 8: statusPrompt()
    elif choice == 9: pollPrompt()
    elif choice == 10: sys.exit(0)
    choice = 0
