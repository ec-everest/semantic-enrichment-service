	0. Cree un usuario everest con permisos de administración. y ejecute todos la instalación desde este usuario. 
	1. Descargue, instale y ponga por defecto Java 8
		sudo yum install java-1.8.0-openjdk.x86_64
		sudo yum install java-1.8.0-openjdk-devel.x86_64
		sudo alternatives --config java (seleccionamos java-1.8.0...)
		sudo alternatives --config javac (seleccionamos java-1.8.0...)
		editar el fichero /etc/profile y adicionar exportar la variable java home (sudo vi /etc/profile)
		export JAVA_HOME=/usr/lib/jvm/java-1.8.0	
		source /etc/profile	
	2. Descargue e instale Maven
		sudo wget http://repos.fedorapeople.org/repos/dchen/apache-maven/epel-apache-maven.repo -O /etc/yum.repos.d/epel-apache-maven.repo
		sudo sed -i s/\$releasever/6/g /etc/yum.repos.d/epel-apache-maven.repo
		sudo yum install -y apache-maven 
	3. Descargue e instale Solr 6.3.0
		sudo wget http://apache.rediris.es/lucene/solr/6.3.0/solr-6.3.0.tgz
		tar xzf solr-6.3.0.tgz solr-6.3.0/bin/install_solr_service.sh --strip-components=2
		sudo bash ./install_solr_service.sh solr-6.3.0.tgz
	4. Conecte con el servidor CVS para descargar los proyectos semanticsearchRO y SolrIndexingRO
		 export CVSROOT=nombre_de_usuario@172.16.32.52:/cvs
		 cvs checkout semanticsearchRO
		 cvs checkout SolrIndexingRO
	5. Inicie Solr
		sudo service solr start
	6. Cree el core EverEst
		cd /opt/solr-6.3.0/bin
		sudo -u solr ./solr create_core -c EverEst
	7. Copie los archivos de la carpeta conf del proyecto SolrIndexingRO en Solr (solr-6.3.0/server/solr/EverEst/conf) y sustituya los archivos antiguos por los nuevos.
		 sudo cp /home/everest/SolrIndexingRO/conf -avt /var/solr/data/EverEst/
	8. Reinicie Solr
		sudo service solr restart
	9. Descargue e instale ESSEX 13.9 y el Dispatcher 13.7
		(Descarga en Windows mediante la wiki)		
		scp GSL_Dispatcher64_13.7.0_ES1.tar.gz root@172.16.32.89:/root
		scp essex_en_13.9u1_int_build_9306_x64.tar.gz root@172.16.32.89:/root
		tar -zxvf GSL_Dispatcher64_13.7.0_ES1.tar.gz
		tar -zxvf essex_en_13.9u1_int_build_9306_x64.tar.gz
		cd ./GSL_Dispatcher64_13.7.0
		sudo ./dispatcher_install.sh /home/everest/Dispatcher
		cd ./essex_en_13.9u1_int_x64/
		sudo ./essex_install.sh /home/everest/essex
	10. Compruebe que se encuentra en Servicios
		chkconfig --list | grep -i Dispatcher
			Debe quedar algo así:
				Dispatcher      0:off   1:off   2:off   3:on    4:on    5:on    6:off
		chkconfig --list | grep -i ESSEX_en1
			Debe quedar algo así:
				ESSEX_en1      	0:off   1:off   2:off   3:on    4:on    5:on    6:off	
				
	(Si aun así no funciona, ejecutar sudo ./startServices)
	
	11. Cree la variable de entorno SENSIGRAFO con ruta en /root/expertsystem/dic/en
		editar el fichero /etc/profile y adicionar exportar la variable SENSIGRAFO (sudo vi /etc/profile)
		export SENSIGRAFO=/home/everest/essex/dic/en
	12. Coloque la librería libsensei.so en /usr/lib
		cd /home/everest/semanticsearRO
		cp libsensei.so /usr/lib
	13. Ejecute semanticsearchRO 
		sudo mvn install
		cd target
		java -jar  -Dlog4j.configurationFile=./classes/conf/log4j2.xml semanticsearchRO-0.0.1-SNAPSHOT-jar-with-dependencies.jar
	
------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

	El programa creará la carpeta SemanticSearchROHub en su carpeta home, la cual contendrá toda la información de los Research Objects a indexar.
	Por favor, no interrumpa la ejecución del programa ni altere la carpeta SemanticSearchROHub hasta que el proceso haya finalizado completamente.
	Su buscador se encontrará en la dirección http://172.16.32.89:8983/solr/EverEst/browse
	
------------------------------ BORRAR TODOS LOS DOCUMENTOS DEL INDICE -----------------------------
 curl http://172.16.32.89:8983/solr/EverEst/update --data '<delete><query>*:*</query></delete>' -H 'Content-type:text/xml; charset=utf-8'
 curl http://localhost:8983/solr/EverEst/update --data '<commit/>' -H 'Content-type:text/xml; charset=utf-8'
 
 En Windows: http://localhost:8983/solr/EverEst/update?stream.body=%3Cdelete%3E%3Cquery%3E*:*%3C/query%3E%3C/delete%3E&commit=true
 
	