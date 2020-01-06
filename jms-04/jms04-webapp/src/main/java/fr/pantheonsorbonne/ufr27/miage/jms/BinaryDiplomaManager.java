package fr.pantheonsorbonne.ufr27.miage.jms;

import java.io.Closeable;
import java.io.IOException;
import java.io.StringWriter;

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
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import fr.pantheonsorbonne.ufr27.miage.DiplomaInfo;
import fr.pantheonsorbonne.ufr27.miage.dto.BinaryDiplomaDTO;

@ApplicationScoped
public class BinaryDiplomaManager implements Closeable {

	@Inject
	@Named("diplomaRequests")
	private Queue requestsQueue;

	@Inject
	@Named("diplomaFiles")
	private Queue filesQueue;

	@Inject
	private ConnectionFactory connectionFactory;

	private Connection connection;
	private MessageConsumer binDiplomaConsumer;
	private MessageProducer diplomaRequestProducer;

	private Session session;

	@PostConstruct
	void init() {
		try {
			connection = connectionFactory.createConnection("nicolas", "nicolas");
			connection.start();
			session = connection.createSession();
			binDiplomaConsumer = session.createConsumer(filesQueue);
			diplomaRequestProducer = session.createProducer(requestsQueue);
		} catch (JMSException e) {
			throw new RuntimeException(e);
		}

	}

	public BinaryDiplomaDTO consume() {

		// receive a Byte Message from the consumer
		// create a byte array sized after the message's payload body length
		// read the message on the byte array
		// create a BinaryDiplomaDTO containing the id of the diploma and the data
		// return the DTO
		try {
			BytesMessage msg = (BytesMessage) binDiplomaConsumer.receive();
			byte[] pld = new byte[(int) msg.getBodyLength()];
			msg.readBytes(pld);

			BinaryDiplomaDTO dto = new BinaryDiplomaDTO();
			dto.setId(msg.getIntProperty("id"));
			dto.setData(pld);
			return dto;

		} catch (JMSException e) {
			System.out.println("Error consume message");
			return null;
		}

	}

	public void requestBinDiploma(DiplomaInfo info) {

		// create a String writer
		// create a JaxBContext, and bount DiplomaInfo.class
		// create a Marshaller and marshall the class in the writer
		// send a text Message containing the JAXB-marshalled object through the wire

		try {
			StringWriter writer = new StringWriter();
			JAXBContext jaxbContext = JAXBContext.newInstance(DiplomaInfo.class);
			jaxbContext.createMarshaller().marshal(info, writer);
			this.diplomaRequestProducer.send(this.session.createTextMessage(writer.toString()));
		} catch (JAXBException | JMSException e) {
			System.out.println("Error");
		}
	}

	@Override
	public void close() throws IOException {
		try {
			binDiplomaConsumer.close();
			session.close();
			connection.close();
		} catch (JMSException e) {
			System.out.println("Failed to close JMS resources");
		}

	}

}
