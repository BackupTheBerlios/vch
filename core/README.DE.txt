Vorraussetzung
===============================================================================
1. Sun Java 1.6 (http://www.java.com/de/)
2. Für die osdserver-Schnittstelle ist entweder das MPlayer-Plugin oder das
   xineliboutput-Plugin notwendig. Das xineliboutput-Plugin muss das 
   SVDRP-Kommando PMDA unterstützen. Die aktuelle Version 1.0.4 tut das noch 
   nicht. 
   

Schnellstart Anleitung
===============================================================================
1. Zip-Archiv entpacken. Z.B. nach /opt
   cd /opt
   unzip /tmp/VodcatcherHelper-xxx.zip (oder wo auch immer man das 
                                        Zip gespeichert hat)
     
2. VCH starten mit bash startServer.sh

   Falls der Port 8080 schon belegt ist, wird es zu einem Fehler kommen.
   In diesem Fall muss man die andere Anwendung auf Port 8080 für den ersten
   Start von Vodcatcher Helper stoppen und dann in der Konfiguration von 
   Vodcatcher Helper den Port ändern. Siehe 3.
   
3. Konfiguration vornehmen
   VodcatcherHelper beinhaltet einen Webserver, mit dem man die Konfiguration
   vornehmen kann. Dazu ruft man in seinem Lieblingsbrowser 
   (ich empfehle Firefox) http://<host>:8080/config auf.
   Eine Hilfe gibt es unter http://<host>:8080/files/help/de_DE/
   
4. Parser starten
   Das Starten des Parsers erfolgt über den integrierten Webserver. 
   Dazu einfach http://<host>:8080/parse im Browser aufrufen. Möchte man
   diesen Vorgang automatisieren (z.B. nachts laufen lassen), kann man das
   mit curl tun (curl http://<host>:8080/parse).
   

Hilfe
===============================================================================
Eine ausführliche Doku gibt es http://<host>:8080/files/help/de_DE/