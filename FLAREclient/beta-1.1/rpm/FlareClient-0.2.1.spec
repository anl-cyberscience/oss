Name:           FlareClient
Version:	0.2.1        
Release:	1%{?dist}
Summary:      	Flare client application for use with Flare TAXII Server. 

License:	GNU AFFERO GENERAL PUBLIC LICENSE

Source0:	FlareClient-0.2.1.tar.gz        
BuildRoot:	/tmp

%description
This is the Flare Client Application for use with the Flare TAXII Server.
It provides both the publisher and subscriber clients.


%prep
%setup -q
%build
%install
install -m 0755 -d $RPM_BUILD_ROOT/root/FlareClient-0.2.1
install -m 0644 config.properties.template $RPM_BUILD_ROOT/root/FlareClient-0.2.1/config.properties.template
install -m 0755 httpsListener $RPM_BUILD_ROOT/root/FlareClient-0.2.1/httpsListener
install -m 0644 log4j.properties $RPM_BUILD_ROOT/root/FlareClient-0.2.1/log4j.properties
install -m 0755 pubDirectory $RPM_BUILD_ROOT/root/FlareClient-0.2.1/pubDirectory
install -m 0755 -d $RPM_BUILD_ROOT/root/FlareClient-0.2.1/lib
install -m 0755 -d $RPM_BUILD_ROOT/root/FlareClient-0.2.1/lib/UnlimitedJCEPolicy
install -m 0644 lib/UnlimitedJCEPolicy/local_policy.jar $RPM_BUILD_ROOT/root/FlareClient-0.2.1/lib/UnlimitedJCEPolicy/local_policy.jar
install -m 0644 lib/UnlimitedJCEPolicy/README.txt $RPM_BUILD_ROOT/root/FlareClient-0.2.1/lib/UnlimitedJCEPolicy/README.txt
install -m 0644 lib/UnlimitedJCEPolicy/US_export_policy.jar $RPM_BUILD_ROOT/root/FlareClient-0.2.1/lib/UnlimitedJCEPolicy/US_export_policy.jar
install -m 0644 lib/cacerts $RPM_BUILD_ROOT/root/FlareClient-0.2.1/lib/cacerts
install -m 0644 lib/commons-io-2.4.jar $RPM_BUILD_ROOT/root/FlareClient-0.2.1/lib/commons-io-2.4.jar
install -m 0644 lib/commons-codec-1.9.jar $RPM_BUILD_ROOT/root/FlareClient-0.2.1/lib/commons-codec-1.9.jar
install -m 0644 lib/flare-client-0.2.1.jar $RPM_BUILD_ROOT/root/FlareClient-0.2.1/lib/flare-client-0.2.1.jar
install -m 0644 lib/log4j-1.2.14.jar $RPM_BUILD_ROOT/root/FlareClient-0.2.1/lib/log4j-1.2.14.jar
install -m 0644 lib/publisherKeyStore $RPM_BUILD_ROOT/root/FlareClient-0.2.1/lib/publisherKeyStore
install -m 0644 lib/subscriberKeyStore $RPM_BUILD_ROOT/root/FlareClient-0.2.1/lib/subscriberKeyStore
install -m 0755 -d $RPM_BUILD_ROOT/root/FlareClient-0.2.1/xml
install -m 0755 -d $RPM_BUILD_ROOT/root/FlareClient-0.2.1/xml/taxii
install -m 0755 -d $RPM_BUILD_ROOT/root/FlareClient-0.2.1/xml/w3
install -m 0644 xml/taxii/taxii1.0.xsd $RPM_BUILD_ROOT/root/FlareClient-0.2.1/xml/taxii/taxii1.0.xsd
install -m 0644 xml/w3/datatypes.dtd $RPM_BUILD_ROOT/root/FlareClient-0.2.1/xml/w3/datatypes.dtd
install -m 0644 xml/w3/xmldsig-core-schema.xsd $RPM_BUILD_ROOT/root/FlareClient-0.2.1/xml/w3/xmldsig-core-schema.xsd
install -m 0644 xml/w3/XMLSchema.dtd $RPM_BUILD_ROOT/root/FlareClient-0.2.1/xml/w3/XMLSchema.dtd
install -m 0755 -d $RPM_BUILD_ROOT/root/FlareClient-0.2.1/publishFeeds
install -m 0755 -d $RPM_BUILD_ROOT/root/FlareClient-0.2.1/subscribeFeeds
install -m 0755 -d $RPM_BUILD_ROOT/root/FlareClient-0.2.1/doc
install -m 0644 doc/allclasses-frame.html $RPM_BUILD_ROOT/root/FlareClient-0.2.1/doc/allclasses-frame.html
install -m 0644 doc/allclasses-noframe.html $RPM_BUILD_ROOT/root/FlareClient-0.2.1/doc/allclasses-noframe.html
install -m 0644 doc/constant-values.html $RPM_BUILD_ROOT/root/FlareClient-0.2.1/doc/constant-values.html
install -m 0644 doc/deprecated-list.html $RPM_BUILD_ROOT/root/FlareClient-0.2.1/doc/deprecated-list.html
install -m 0644 doc/help-doc.html $RPM_BUILD_ROOT/root/FlareClient-0.2.1/doc/help-doc.html
install -m 0644 doc/index.html $RPM_BUILD_ROOT/root/FlareClient-0.2.1/doc/index.html
install -m 0644 doc/overview-frame.html $RPM_BUILD_ROOT/root/FlareClient-0.2.1/doc/overview-frame.html
install -m 0644 doc/overview-summary.html $RPM_BUILD_ROOT/root/FlareClient-0.2.1/doc/overview-summary.html
install -m 0644 doc/overview-tree.html $RPM_BUILD_ROOT/root/FlareClient-0.2.1/doc/overview-tree.html
install -m 0644 doc/package-list $RPM_BUILD_ROOT/root/FlareClient-0.2.1/doc/package-list
install -m 0644 doc/stylesheet.css $RPM_BUILD_ROOT/root/FlareClient-0.2.1/doc/stylesheet.css
install -m 0755 -d $RPM_BUILD_ROOT/root/FlareClient-0.2.1/doc/com
install -m 0755 -d $RPM_BUILD_ROOT/root/FlareClient-0.2.1/doc/com/bcmcgroup
install -m 0755 -d $RPM_BUILD_ROOT/root/FlareClient-0.2.1/doc/com/bcmcgroup/flare
install -m 0755 -d $RPM_BUILD_ROOT/root/FlareClient-0.2.1/doc/com/bcmcgroup/taxii
install -m 0755 -d $RPM_BUILD_ROOT/root/FlareClient-0.2.1/doc/com/bcmcgroup/flare/client
install -m 0644 doc/com/bcmcgroup/flare/client/ConfigClient.html $RPM_BUILD_ROOT/root/FlareClient-0.2.1/doc/com/bcmcgroup/flare/client/ConfigClient.html
install -m 0644 doc/com/bcmcgroup/flare/client/HttpsListener.html $RPM_BUILD_ROOT/root/FlareClient-0.2.1/doc/com/bcmcgroup/flare/client/HttpsListener.html
install -m 0644 doc/com/bcmcgroup/flare/client/package-frame.html $RPM_BUILD_ROOT/root/FlareClient-0.2.1/doc/com/bcmcgroup/flare/client/package-frame.html
install -m 0644 doc/com/bcmcgroup/flare/client/package-summary.html $RPM_BUILD_ROOT/root/FlareClient-0.2.1/doc/com/bcmcgroup/flare/client/package-summary.html
install -m 0644 doc/com/bcmcgroup/flare/client/package-tree.html $RPM_BUILD_ROOT/root/FlareClient-0.2.1/doc/com/bcmcgroup/flare/client/package-tree.html
install -m 0644 doc/com/bcmcgroup/flare/client/package-use.html $RPM_BUILD_ROOT/root/FlareClient-0.2.1/doc/com/bcmcgroup/flare/client/package-use.html
install -m 0644 doc/com/bcmcgroup/flare/client/PubDirectory.html $RPM_BUILD_ROOT/root/FlareClient-0.2.1/doc/com/bcmcgroup/flare/client/PubDirectory.html
install -m 0644 doc/com/bcmcgroup/flare/client/Publisher.html $RPM_BUILD_ROOT/root/FlareClient-0.2.1/doc/com/bcmcgroup/flare/client/Publisher.html
install -m 0644 doc/com/bcmcgroup/flare/client/Subscriber.html $RPM_BUILD_ROOT/root/FlareClient-0.2.1/doc/com/bcmcgroup/flare/client/Subscriber.html
install -m 0755 -d $RPM_BUILD_ROOT/root/FlareClient-0.2.1/doc/com/bcmcgroup/flare/client/class-use
install -m 0644 doc/com/bcmcgroup/flare/client/class-use/ConfigClient.html $RPM_BUILD_ROOT/root/FlareClient-0.2.1/doc/com/bcmcgroup/flare/client/class-use/ConfigClient.html
install -m 0644 doc/com/bcmcgroup/flare/client/class-use/HttpsListener.html $RPM_BUILD_ROOT/root/FlareClient-0.2.1/doc/com/bcmcgroup/flare/client/class-use/HttpsListener.html
install -m 0644 doc/com/bcmcgroup/flare/client/class-use/PubDirectory.html $RPM_BUILD_ROOT/root/FlareClient-0.2.1/doc/com/bcmcgroup/flare/client/class-use/PubDirectory.html
install -m 0644 doc/com/bcmcgroup/flare/client/class-use/Publisher.html $RPM_BUILD_ROOT/root/FlareClient-0.2.1/doc/com/bcmcgroup/flare/client/class-use/Publisher.html
install -m 0644 doc/com/bcmcgroup/flare/client/class-use/Subscriber.html $RPM_BUILD_ROOT/root/FlareClient-0.2.1/doc/com/bcmcgroup/flare/client/class-use/Subscriber.html
install -m 0644 doc/com/bcmcgroup/taxii/ContentBlock.html $RPM_BUILD_ROOT/root/FlareClient-0.2.1/doc/com/bcmcgroup/taxii/ContentBlock.html
install -m 0644 doc/com/bcmcgroup/taxii/package-frame.html $RPM_BUILD_ROOT/root/FlareClient-0.2.1/doc/com/bcmcgroup/taxii/package-frame.html
install -m 0644 doc/com/bcmcgroup/taxii/package-summary.html $RPM_BUILD_ROOT/root/FlareClient-0.2.1/doc/com/bcmcgroup/taxii/package-summary.html
install -m 0644 doc/com/bcmcgroup/taxii/package-tree.html $RPM_BUILD_ROOT/root/FlareClient-0.2.1/doc/com/bcmcgroup/taxii/package-tree.html
install -m 0644 doc/com/bcmcgroup/taxii/package-use.html $RPM_BUILD_ROOT/root/FlareClient-0.2.1/doc/com/bcmcgroup/taxii/package-use.html
install -m 0644 doc/com/bcmcgroup/taxii/StatusMessage.html $RPM_BUILD_ROOT/root/FlareClient-0.2.1/doc/com/bcmcgroup/taxii/StatusMessage.html
install -m 0755 -d $RPM_BUILD_ROOT/root/FlareClient-0.2.1/doc/com/bcmcgroup/flare/taxii/class-use
#install -m 0644 doc/com/bcmcgroup/taxii/class-use/ContentBlock.html $RPM_BUILD_ROOT/root/FlareClient-0.2.1/doc/com/bcmcgroup/taxii/class-use/ContentBlock.html
#install -m 0644 doc/com/bcmcgroup/taxii/class-use/StatusMessage.html $RPM_BUILD_ROOT/root/FlareClient-0.2.1/doc/com/bcmcgroup/taxii/class-use/StatusMessage.html
install -m 0755 -d $RPM_BUILD_ROOT/root/FlareClient-0.2.1/doc/index-files
install -m 0644 doc/index-files/index-1.html $RPM_BUILD_ROOT/root/FlareClient-0.2.1/doc/index-files/index-1.html
install -m 0644 doc/index-files/index-2.html $RPM_BUILD_ROOT/root/FlareClient-0.2.1/doc/index-files/index-2.html
install -m 0644 doc/index-files/index-3.html $RPM_BUILD_ROOT/root/FlareClient-0.2.1/doc/index-files/index-3.html
install -m 0644 doc/index-files/index-4.html $RPM_BUILD_ROOT/root/FlareClient-0.2.1/doc/index-files/index-4.html
install -m 0644 doc/index-files/index-5.html $RPM_BUILD_ROOT/root/FlareClient-0.2.1/doc/index-files/index-5.html
install -m 0644 doc/index-files/index-6.html $RPM_BUILD_ROOT/root/FlareClient-0.2.1/doc/index-files/index-6.html
install -m 0644 doc/index-files/index-7.html $RPM_BUILD_ROOT/root/FlareClient-0.2.1/doc/index-files/index-7.html
install -m 0755 -d $RPM_BUILD_ROOT/root/FlareClient-0.2.1/doc/resources
install -m 0644 doc/resources/background.gif $RPM_BUILD_ROOT/root/FlareClient-0.2.1/doc/resources/background.gif
install -m 0644 doc/resources/tab.gif $RPM_BUILD_ROOT/root/FlareClient-0.2.1/doc/resources/tab.gif
install -m 0644 doc/resources/titlebar_end.gif $RPM_BUILD_ROOT/root/FlareClient-0.2.1/doc/resources/titlebar_end.gif
install -m 0644 doc/resources/titlebar.gif $RPM_BUILD_ROOT/root/FlareClient-0.2.1/doc/resources/titlebar.gif

%clean
#rm -rf $RPM_BUILD_ROOT

%files
%dir /root/FlareClient-0.2.1
%dir /root/FlareClient-0.2.1/lib
%dir /root/FlareClient-0.2.1/lib/UnlimitedJCEPolicy
%dir /root/FlareClient-0.2.1/xml
%dir /root/FlareClient-0.2.1/xml/taxii
%dir /root/FlareClient-0.2.1/xml/w3
%dir /root/FlareClient-0.2.1/publishFeeds
%dir /root/FlareClient-0.2.1/subscribeFeeds
%dir /root/FlareClient-0.2.1/doc
%dir /root/FlareClient-0.2.1/doc/com
%dir /root/FlareClient-0.2.1/doc/com/bcmcgroup
%dir /root/FlareClient-0.2.1/doc/com/bcmcgroup/flare
%dir /root/FlareClient-0.2.1/doc/com/bcmcgroup/taxii
%dir /root/FlareClient-0.2.1/doc/com/bcmcgroup/flare/client
%dir /root/FlareClient-0.2.1/doc/com/bcmcgroup/flare/client/class-use
%dir /root/FlareClient-0.2.1/doc/com/bcmcgroup/flare/taxii/class-use
%dir /root/FlareClient-0.2.1/doc/index-files
%dir /root/FlareClient-0.2.1/doc/resources
/root/FlareClient-0.2.1/config.properties.template
/root/FlareClient-0.2.1/httpsListener
/root/FlareClient-0.2.1/log4j.properties
/root/FlareClient-0.2.1/pubDirectory
/root/FlareClient-0.2.1/lib/UnlimitedJCEPolicy/local_policy.jar
/root/FlareClient-0.2.1/lib/UnlimitedJCEPolicy/README.txt
/root/FlareClient-0.2.1/lib/UnlimitedJCEPolicy/US_export_policy.jar
/root/FlareClient-0.2.1/lib/cacerts
/root/FlareClient-0.2.1/lib/commons-io-2.4.jar
/root/FlareClient-0.2.1/lib/commons-codec-1.9.jar
/root/FlareClient-0.2.1/lib/flare-client-0.2.1.jar
/root/FlareClient-0.2.1/lib/log4j-1.2.14.jar
/root/FlareClient-0.2.1/lib/publisherKeyStore
/root/FlareClient-0.2.1/lib/subscriberKeyStore
/root/FlareClient-0.2.1/xml/taxii/taxii1.0.xsd
/root/FlareClient-0.2.1/xml/w3/datatypes.dtd
/root/FlareClient-0.2.1/xml/w3/xmldsig-core-schema.xsd
/root/FlareClient-0.2.1/xml/w3/XMLSchema.dtd
/root/FlareClient-0.2.1/doc/allclasses-frame.html
/root/FlareClient-0.2.1/doc/allclasses-noframe.html
/root/FlareClient-0.2.1/doc/constant-values.html
/root/FlareClient-0.2.1/doc/deprecated-list.html
/root/FlareClient-0.2.1/doc/help-doc.html
/root/FlareClient-0.2.1/doc/index.html
/root/FlareClient-0.2.1/doc/overview-frame.html
/root/FlareClient-0.2.1/doc/overview-summary.html
/root/FlareClient-0.2.1/doc/overview-tree.html
/root/FlareClient-0.2.1/doc/package-list
/root/FlareClient-0.2.1/doc/stylesheet.css
/root/FlareClient-0.2.1/doc/com/bcmcgroup/flare/client/ConfigClient.html
/root/FlareClient-0.2.1/doc/com/bcmcgroup/flare/client/HttpsListener.html
/root/FlareClient-0.2.1/doc/com/bcmcgroup/flare/client/package-frame.html
/root/FlareClient-0.2.1/doc/com/bcmcgroup/flare/client/package-summary.html
/root/FlareClient-0.2.1/doc/com/bcmcgroup/flare/client/package-tree.html
/root/FlareClient-0.2.1/doc/com/bcmcgroup/flare/client/package-use.html
/root/FlareClient-0.2.1/doc/com/bcmcgroup/flare/client/PubDirectory.html
/root/FlareClient-0.2.1/doc/com/bcmcgroup/flare/client/Publisher.html
/root/FlareClient-0.2.1/doc/com/bcmcgroup/flare/client/Subscriber.html
/root/FlareClient-0.2.1/doc/com/bcmcgroup/flare/client/class-use/ConfigClient.html
/root/FlareClient-0.2.1/doc/com/bcmcgroup/flare/client/class-use/HttpsListener.html
/root/FlareClient-0.2.1/doc/com/bcmcgroup/flare/client/class-use/PubDirectory.html
/root/FlareClient-0.2.1/doc/com/bcmcgroup/flare/client/class-use/Publisher.html
/root/FlareClient-0.2.1/doc/com/bcmcgroup/flare/client/class-use/Subscriber.html
/root/FlareClient-0.2.1/doc/com/bcmcgroup/taxii/ContentBlock.html
/root/FlareClient-0.2.1/doc/com/bcmcgroup/taxii/package-frame.html
/root/FlareClient-0.2.1/doc/com/bcmcgroup/taxii/package-summary.html
/root/FlareClient-0.2.1/doc/com/bcmcgroup/taxii/package-tree.html
/root/FlareClient-0.2.1/doc/com/bcmcgroup/taxii/package-use.html
/root/FlareClient-0.2.1/doc/com/bcmcgroup/taxii/StatusMessage.html
#/root/FlareClient-0.2.1/doc/com/bcmcgroup/taxii/class-use/ContentBlock.html
#/root/FlareClient-0.2.1/doc/com/bcmcgroup/taxii/class-use/StatusMessage.html
/root/FlareClient-0.2.1/doc/index-files/index-1.html
/root/FlareClient-0.2.1/doc/index-files/index-2.html
/root/FlareClient-0.2.1/doc/index-files/index-3.html
/root/FlareClient-0.2.1/doc/index-files/index-4.html
/root/FlareClient-0.2.1/doc/index-files/index-5.html
/root/FlareClient-0.2.1/doc/index-files/index-6.html
/root/FlareClient-0.2.1/doc/index-files/index-7.html
/root/FlareClient-0.2.1/doc/resources/background.gif
/root/FlareClient-0.2.1/doc/resources/tab.gif
/root/FlareClient-0.2.1/doc/resources/titlebar_end.gif
/root/FlareClient-0.2.1/doc/resources/titlebar.gif

%defattr(-,root,root,-)
%doc
%changelog
