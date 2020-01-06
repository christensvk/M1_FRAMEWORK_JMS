package fr.pantheonsorbonne.miage.jms;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import fr.pantheonsorbonne.miage.diploma.DiplomaGenerator;
import fr.pantheonsorbonne.miage.diploma.MiageDiplomaGenerator;
import fr.pantheonsorbonne.ufr27.miage.DiplomaInfo;

@ApplicationScoped
public class PdfGeneratorMessageHandler implements Closeable {

	@Inject
	@Named("diplomaRequests")
	private Queue requestsQueue;

	@Inject
	@Named("diplomaFiles")
	private Queue filesQueue;

	@Inject
	private ConnectionFactory connectionFactory;

	private Connection connection;
	private MessageConsumer diplomaRequestConsummer;
	private MessageProducer diplomaFileProducer;

	private Session session;

	@PostConstruct
	void init() {
		try {
			connection = connectionFactory.createConnection("nicolas", "nicolas");
			connection.start();
			session = connection.createSession();
			diplomaRequestConsummer = session.createConsumer(requestsQueue);
			diplomaFileProducer = session.createProducer(filesQueue);

		} catch (JMSException e) {
			throw new RuntimeException(e);
		}

	}

	public void consume() {
		
		// receive a text message from the consummer
		// create a jaxbcontext, binding the DiplomaInfo class
		// unmarshall the texte message body with the JaxBcontext
		// use the handleReceivedDiplomaSpect method to generate the diploma an send it through the wire
		try {
			TextMessage msg = (TextMessage) diplomaRequestConsummer.receive();
			JAXBContext jaxbContext = JAXBContext.newInstance(DiplomaInfo.class);
			DiplomaInfo diplome = (DiplomaInfo) jaxbContext.createUnmarshaller().unmarshal(new StringReader(msg.getText()));

			handledReceivedDiplomaSpect(diplome);

		} catch (JAXBException | JMSException e) {
			System.out.println("Error consume message");

		}		
	}

	private void handledReceivedDiplomaSpect(DiplomaInfo diplome) {

		// create a new MIageDiplomaGenerator Instance from the diploma Info
		// get the content (inputstream ) from the generator
		// create an array of bytes having the size of the inputstream
		// read the inputstream data into the adday
		// use the sendBinary sendBinaryDiploma function to send the diploma through the
		// write
		// close the IS

		try {
			DiplomaGenerator diplomaGenerator = new MiageDiplomaGenerator(diplome.getStudent());
			InputStream flux = diplomaGenerator.getContent();
			byte[] data = new byte[flux.available()];
			flux.read(data);
			this.sendBinaryDiploma(diplome, data);
			flux.close();
		} catch (IOException e) {
			System.err.println("Error send request");
		}
	}

	public void sendBinaryDiploma(DiplomaInfo info, byte[] data) {
		//	create a new byte message using the session object
		// set an IntProperty on the message containing the id of the diploma
		// write the bytes into the bytesmessage
		// send the message through the producer

		try {
			BytesMessage message = this.session.createBytesMessage();
			message.setIntProperty("id", info.getId());
			message.writeBytes(data);

			this.diplomaFileProducer.send(message);

		} catch (JMSException e) {
			System.err.println("Error send request");
		}

	}

	@Override
	public void close() throws IOException {
		try {
			diplomaFileProducer.close();
			diplomaRequestConsummer.close();
			session.close();
			connection.close();
		} catch (JMSException e) {
			System.out.println("Failed to close JMS resources");
		}

	}

}
