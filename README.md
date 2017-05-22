# semantic-enrichment-service

The semantic enrichment service goal is to enhance the research object findability by adding to the user-generated annotations new semantic metadata that is automatically gathered from research object content, more specifically from the resources containing textual content. 

The semantic enrichment produces metadata out of the textual content in the research object regarding: 

* Main Concepts: Most frequent concepts mentioned in the text.
*	Main Domains: Fields of knowledge in which the main concepts are most commonly used
*	Main Lemmas: Most frequent lemmas found in the text.
*	Main Compound Terms: Most relevant phrases or collocations found in the text.
*	Named entities: all the named entities found in the text classified into People, Organizations and Places. 

All these metadata types are added to the research object as annotations. The annotations are written according to the content description vocabulary `https://w3id.org/contentdesc`.

# Installation

The semantic enrichment service is fully implemented in Java 8 and uses maven 3 to manage the libraries dependencies and generate the jar and war files that are going to be deployed. The enrichment service is implemented as a rest api using the framework Jersey 2 that can be deployed on any servlet container supporting Servlet 2.5 and higher, such as Tomcat 8.  

The enrichment service uses rabbitmq as message broker. Therefore an installation of rabbitmq 3.6 or above should be installed. Finally, a solr index is used to stored the research objects and their enrichments. The solr version required is 6.5 or above. 

To install the enrichment service first clone this repository using `git clone`

Then use `maven install` to compile the enrichment service in the root folder. This command generates different jar files and war files. 

* The file `enricher.war`, that contains the **enricher web service**, in the target folder of the module `/everest-github-enricher-restws` must be deployed in the servlet container in the path `/ro`. 

* The file `everest-github-enricher-request-0.0.1-SNAPSHOT-jar-with-dependencies.jar` in the target folder and the file `enrichment_request.sh` of the module `/everest-github-enricher-request` must be copied to the server and the latter run as regular sh file. **This daemon is in charge of processing the enrichment request received through the web service**.

* The file `everest-github-enricher-response-0.0.1-SNAPSHOT-jar-with-dependencies.jar` in the target folder and the file `enrichment_response.sh` of the module `/everest-github-enricher-response` must be copied to the server and the latter run as regular sh file. This **daemon is in charge of post the request of the callbacks with the annotations generated**.

* The `everest-github-enricher-indexer-0.0.1-SNAPSHOT-jar-with-dependencies.jar` in the target folder and the file `enrichment_toindex.sh` of the module `/everest-github-enricher-indexer` must be copied to the server and the latter run as regular sh file. **This daemon is in charge of indexing the research objects along with their annotations**.

* The configuration of the solr scheme required to work with all the components of the enrichment services must be copied from the folder `conf` in the module `/everest-github-enricher-solr/conf`


# Web API

The semantic enrichment service API was designed according to the callback asynchronous invocation model. This model is used when the processing may be long, which is the case of the semantic enrichment given its complexity, and the client side does not want to get blocked until a response is sent back. The general idea is that the client system sends along with the enrichment request a callback method that is called by the server side to send the response to the client system.   

The API is implemented a rest web service that accept `POST` request where the body of the request is a json document containing three elements: `ro_uri, callback and nonce`. The element `ro_uri` is the uri of the research object, the `callback` is the uri of the callback method, and `nonce` is a value that the enrichment service will return when calling the callback service.  The callback service must accept `POST` request with the authentication header specifying the access token (`bearer access-token`) and the body of the request contains the annotations in turtle format generated by the enrichment service. Below an example of use of the service is presented:

```
POST /ro/enrichment HTTP/1.1
HOST: everest.expertsystemlab.com
content-type: application/json
message body:
{
"ro_uri":"http://sandbox.rohub.org/rodl/ROs/LandMonitoring_Change_Detecting/",
 "callback":"http://example.org/callback",
 "nonce":"95483xv1q56"
}
```

When the enrichment services has generated the annotations of the research object calls the callback service using a post request as follows:

```
POST /callback HTTP/1.1
HOST: http://example.org
content-type: text/plain
authentication: bearer <access-token>
message body: <annotations in turtle>
```
