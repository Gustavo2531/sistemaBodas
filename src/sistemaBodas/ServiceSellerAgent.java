package sistemaBodas;

import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import java.util.*;

public class ServiceSellerAgent extends Agent {

	private Hashtable catalogue;

	private Hashtable catalogueMin;
	private ServiceSellerGui myGui;
	private Hashtable numberOfOrders;
	// Put agent initializations here
	protected void setup() {
		// Create the catalogue
		catalogue = new Hashtable();
		catalogueMin = new Hashtable();
		
		numberOfOrders = new Hashtable();
		
		// Create and show the GUI 
		myGui = new ServiceSellerGui(this);
		myGui.showGui();

		
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("service-selling");
		sd.setName("JADE-wedding-service-trading");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}

		// Add the behaviour serving queries from buyer agents
		addBehaviour(new OfferRequestsServer());

		// Add the behaviour serving purchase orders from buyer agents
		addBehaviour(new PurchaseOrdersServer());
	}

	// Put agent clean-up operations here
	protected void takeDown() {
		// Deregister from the yellow pages
		try {
			DFService.deregister(this);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
		// Close the GUI
		myGui.dispose();
		// Printout a dismissal message
		System.out.println("Seller-agent "+getAID().getName()+" terminating.");
	}


	
	public void updateCatalogue(final String title, final int price, final int minPrice) {
		addBehaviour(new OneShotBehaviour() {
			public void action() {
				catalogue.put(title, new Integer(price));
				catalogueMin.put(title, new Integer(minPrice));
				
				System.out.println(title+" inserted into catalogue. Price = "+price);
			}
		} );
	}

	
	private class OfferRequestsServer extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				// CFP Message received. Process it
				String title = msg.getContent();
				ACLMessage reply = msg.createReply();

				Integer price = (Integer) catalogue.get(title);
				Integer minPrice = (Integer) catalogueMin.get(title);
				if (price != null) {
					numberOfOrders.put(msg.getSender().getName(), 0);
					reply.setPerformative(ACLMessage.PROPOSE);
					reply.setContent(String.valueOf(price.intValue()));
				}
				else {
					
					reply.setPerformative(ACLMessage.REFUSE);
					reply.setContent("not-available");
				}
				myAgent.send(reply);
			}
			else {
				block();
			}
		}
	}  // End of inner class OfferRequestsServer

	private class OfferRequestsServer2 extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
			ACLMessage msg = myAgent.receive(mt);
			MessageTemplate mp = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);
			ACLMessage msgp = myAgent.receive(mp);
			if (msg != null && msgp != null) {
				// CFP Message received. Process it
				String title = msg.getContent();
				ACLMessage reply = msg.createReply();
				
				String spriceW = msgp.getContent();
				ACLMessage reply2 = msgp.createReply();
				
				Integer priceW = Integer.parseInt(spriceW);

				Integer price = (Integer) catalogue.get(title);
				Integer minPrice = (Integer) catalogueMin.get(title);
				Integer numberOrders = (Integer) numberOfOrders.get(msgp.getSender().getName());
				
				if (priceW < price && priceW > minPrice && numberOfOrders.get(msgp.getSender().getName())!=null) {
					
					reply.setPerformative(ACLMessage.PROPOSE);
					reply.setContent(String.valueOf(priceW.intValue()));
				}
				else {
					if(numberOfOrders.get(msgp.getSender().getName())==null){
						numberOfOrders.put(msgp.getSender().getName(), 0);
					}
					numberOrders = (Integer) numberOfOrders.get(msgp.getSender().getName());
					if(numberOrders<3){
						numberOfOrders.put(msgp.getSender().getName(), numberOrders+1);
					}else{
						reply.setPerformative(ACLMessage.REFUSE);
						reply.setContent("not-available");
					}
				}
				myAgent.send(reply);
			}
			else {
				block();
			}
		}
	} 
	private class PurchaseOrdersServer extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				// ACCEPT_PROPOSAL Message received. Process it
				String title = msg.getContent();
				ACLMessage reply = msg.createReply();

				Integer price = (Integer) catalogue.remove(title);
				if (price != null) {
					reply.setPerformative(ACLMessage.INFORM);
					System.out.println(title+" sold to agent "+msg.getSender().getName());
				}
				else {
					
					reply.setPerformative(ACLMessage.FAILURE);
					reply.setContent("not-available");
				}
				myAgent.send(reply);
			}
			else {
				block();
			}
		}
	}  // End of inner class OfferRequestsServer
}
