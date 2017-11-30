package sistemaBodas;

import jade.core.Agent;

import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;


public class WeddingBuyerAgent extends Agent {
	// The title of the wedding Service
	private String targetServiceTitle;
	// The list of known seller agents
	private AID[] sellerAgents;
	 
	public double budget; 
	public double servicePrice; 


	// Put agent initializations here
	protected void setup() {
		budget = Math.random() * (10000.00- 5000.00); 
		System.out.println("Comprador " + getAID().getName() + " listo con presupuesto de: " + budget); 
		
		Object[] args = getArguments();
		if (args != null && args.length > 0) {
			targetServiceTitle = (String) args[0];
			System.out.println("Target wedding is "+targetServiceTitle);
			// Add a TickerBehaviour that schedules a request to seller agents every minute
			addBehaviour(new TickerBehaviour(this, 60000) {
				protected void onTick() {
					System.out.println("Trying to buy "+targetServiceTitle);
					// Update the list of seller agents
					DFAgentDescription template = new DFAgentDescription();
					ServiceDescription sd = new ServiceDescription();
					sd.setType("service-selling");
					template.addServices(sd);
					try {
						DFAgentDescription[] result = DFService.search(myAgent, template); 
						System.out.println("Found the following seller agents:");
						sellerAgents = new AID[result.length];
						for (int i = 0; i < result.length; ++i) {
							sellerAgents[i] = result[i].getName();
							System.out.println(sellerAgents[i].getName());
						}
					}
					catch (FIPAException fe) {
						fe.printStackTrace();
					}

					// Perform the request
					myAgent.addBehaviour(new RequestPerformer());
				}
			} );
		}else {
			// Make the agent terminate
			System.out.println("No target service title specified");
			doDelete();
		}
	}

	// Put agent clean-up operations here
	protected void takeDown() {
		// Printout a dismissal message
		System.out.println("Comprador "+getAID().getName()+" finaliza.");
	}


	private class RequestPerformer extends Behaviour {
		private AID bestSeller; // The agent who provides the best offer 
		private int bestPrice;  // The best offered price
		private int repliesCnt = 0; // The counter of replies from seller agents
		private MessageTemplate mt; // The template to receive replies
		private int step = 0;
		ACLMessage msg;
		ACLMessage reply; 

		public void action() {
			switch (step) {
			case 0:
				// Send the cfp to all sellers
				ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
				for (int i = 0; i < sellerAgents.length; ++i) {
					cfp.addReceiver(sellerAgents[i]);
				} 
				cfp.setContent(targetServiceTitle);
				cfp.setConversationId("service-trade");
				cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
				myAgent.send(cfp);
				// Prepare the template to get proposals
				mt = MessageTemplate.and(MessageTemplate.MatchConversationId("service-trade"),
						MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
				step = 1;
				break;
			case 1:
				// Receive all proposals/refusals from seller agents
				ACLMessage reply = myAgent.receive(mt);
				if (reply != null) {
					// Reply received
					if (reply.getPerformative() == ACLMessage.PROPOSE) {
						// This is an offer 
						int price = Integer.parseInt(reply.getContent());
						if (bestSeller == null || price < bestPrice) {
							// This is the best offer at present
							bestPrice = price;
							bestSeller = reply.getSender();
						}
					}
					repliesCnt++;
					if (repliesCnt >= sellerAgents.length) {
						// We received all replies
						step = 2; 
					}
				}
				else {
					block();
				}
				break;
			case 2: 
				/**ACLMessage cfp2 = new ACLMessage(ACLMessage.CFP);
				for (int i = 0; i < sellerAgents.length; ++i) {
					cfp2.addReceiver(sellerAgents[i]);
				} 
				cfp2.setContent(targetServiceTitle);
				cfp2.setConversationId("service-trade");
				cfp2.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
				System.out.println("Entre isa");**/
				MessageTemplate mt1 = MessageTemplate.MatchConversationId("service-trade");
				ACLMessage reply2 = myAgent.receive(mt1);
				if(reply2 != null){
					
						if (reply2.getPerformative() == ACLMessage.PROPOSE) {
						
							reply = reply2.createReply(); 
							servicePrice = Double.parseDouble(reply2.getContent());
							if(servicePrice < budget || servicePrice == budget){
								reply.setPerformative(ACLMessage.PROPOSE);
								reply.setContent(String.valueOf(servicePrice-100));
							}else{
								reply.setPerformative(ACLMessage.REFUSE);
								reply.setContent("No se acepta");
							}
							
							//myAgent.send(cfp);
							myAgent.send(reply);
							
						}else{
							step = 3; 
						}
					
				}else{
					block();
				}
					
				break; 
				
			case 3:
				// Send the purchase order to the seller that provided the best offer
				ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
				order.addReceiver(bestSeller);
				order.setContent(targetServiceTitle);
				order.setConversationId("service-trade");
				order.setReplyWith("order"+System.currentTimeMillis());
				myAgent.send(order);
				// Prepare the template to get the purchase order reply
				mt = MessageTemplate.and(MessageTemplate.MatchConversationId("service-trade"),
						MessageTemplate.MatchInReplyTo(order.getReplyWith()));
				step = 4;
				break;
			case 4:      
				// Receive the purchase order reply
				reply = myAgent.receive(mt);
				if (reply != null) {
					// Purchase order reply received
					if (reply.getPerformative() == ACLMessage.INFORM) {
						// Purchase successful. We can terminate
						System.out.println(targetServiceTitle+" successfully purchased from agent "+reply.getSender().getName());
						System.out.println("Price = "+bestPrice);
						myAgent.doDelete();
					}
					else {
						System.out.println("Attempt failed: requested service already sold.");
					}

					step = 5;
				}
				else {
					block();
				}
				break;
			}        
		}

		public boolean done() {
			if (step == 2 && bestSeller == null) {
				System.out.println("Attempt failed: "+targetServiceTitle+" not available for sale");
			}
			return ((step == 2 && bestSeller == null) || step == 4);
		}
	}  // End of inner class RequestPerformer
}



