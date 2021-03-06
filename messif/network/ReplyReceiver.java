/*
 *  This file is part of MESSIF library.
 *
 *  MESSIF library is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  MESSIF library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MESSIF library.  If not, see <http://www.gnu.org/licenses/>.
 */
package messif.network;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A {@link Receiver} for receiving reply messages.
 * This class is automatically created and maintained by {@link MessageDispatcher}.
 * It is returned by {@link MessageDispatcher#sendMessageWaitReply sendMessageWaitReply} method
 * and getter methods can be used to receive the responses (or wait for them).
 * 
 *
 * @param <TReplyMessage> type of the reply message that this receiver accepts
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public final class ReplyReceiver<TReplyMessage extends ReplyMessage> implements Receiver {

    /** Logger */
    protected static Logger log = Logger.getLogger("messif.network");


    //****************** Attributes for reply waiting ******************//

    /** Class of the reply messages to await */
    private final Class<? extends TReplyMessage> replyClass;

    /** List of received reply messages */
    private final List<TReplyMessage> messages = new ArrayList<TReplyMessage>();

    /** Set of network nodes from which this receiver expects response */
    private final Set<List<NetworkNode>> waitingPaths;

    /** Set of network nodes that we have received response from, but that are not commited yet */
    private final List<TReplyMessage> uncommitedMessages;

    /** Reply receiver list where this receiver is registered */
    private final ReplyReceiverList registeredToList;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of ReplyReceiver.
     * This method is only called from {@link MessageDispatcher} for within
     * {@link MessageDispatcher#sendMessageWaitReply sendMessageWaitReply} method.
     *
     * @param replyClass the class of reply messages to await (and all its subclasses)
     * @param registeredToList the reply receiver list where this receiver is registered (and will deregister itself after getReplies)
     */
    ReplyReceiver(Class<? extends TReplyMessage> replyClass, ReplyReceiverList registeredToList) {
        waitingPaths = new HashSet<List<NetworkNode>>();
        uncommitedMessages = new ArrayList<TReplyMessage>();
        this.replyClass = replyClass;
        this.registeredToList = registeredToList;
    }


    //******************** Type casting *********************//


    /**
     * Cast a given {@link ReplyReceiver} to a correct generic type.
     * The receiver's reply class is checked so this cast is type-safe.
     *
     * @param <E> the reply messages that the cast receiver waits for
     * @param receiver the receiver to cast
     * @param replyMessageClass the class of the reply messages that the cast receiver waits for
     * @return a safe-cast reply receiver
     */
    @SuppressWarnings("unchecked")
    public static <E extends ReplyMessage> ReplyReceiver<E> cast(ReplyReceiver<?> receiver, Class<? extends E> replyMessageClass) {
        if (receiver == null)
            return null;
        if (replyMessageClass.isAssignableFrom(receiver.replyClass))
            return (ReplyReceiver<E>)receiver;
        else
            throw new ClassCastException("Receiver is waiting for messages of " + receiver.replyClass + " and not " + replyMessageClass.getName());    }


    //******************** Attribute access method *********************//

    /**
     * Returns the number of replies still pending. The returned number may not be precise (can return lower value),
     * but it will be always greater than zero if the response is incomplete.
     * @return number of replies still pending
     */
    public int getRemainingCount() {
        return waitingPaths.size();
    }

    /**
     * Returns <tt>true</tt> if the reply waiting is finished. In other words, it is not waiting for more replies from any node.
     * @return <tt>true</tt> if the reply waiting is finished or <tt>false</tt> if some responses are still missing.
     */
    public boolean isFinished() {
        return getRemainingCount() <= 0;
    }

    /**
     * Return <tt>true</tt> if this receiver is ready to be deregistered from the list of waiting receivers.
     * @return <tt>true</tt> if this receiver is ready to be deregistered from the list of waiting receivers
     */
    public boolean isReadyToRemove() {
        return (registeredToList == null) && isFinished();
    }


    //******************** Management of the reply waiting *********************//

    /**
     * Internal method to add a navigation path to the waiting list.
     * The list of network nodes is a path from which one reply message should arrive.
     * This method is only called from {@link MessageDispatcher} for within
     * {@link MessageDispatcher#sendMessageWaitReply sendMessageWaitReply} method.
     *
     * @param path a path (the sequence of network nodes) that should the reply message arrive from
     */
    void addWaitingPath(List<NetworkNode> path) {
        waitingPaths.add(path);
    }

    /**
     * Internal method to delete a navigation path from the waiting list.
     * The list of network nodes is a path from which one reply message arrived,
     * and thus it can be removed from the waiting list.
     *
     * @param path a path (the sequence of network nodes) that should the reply message arrive from
     * @return true if the path was in the list of waiting nodes
     */
    private boolean removeWaitingPath(List<NetworkNode> path) {
        return waitingPaths.remove(path);
    }

    /**
     * This method replaces the "sender" path by prolonging it by the set of specified destination nodes.
     * Specifically, the original path is removed and, for every network node in <code>notWaitingDestinations</code>
     * a new path (sender path with this network node appended) is added to the waiting path list.
     *
     * @param senderPath the sender path to be replaced
     * @param notWaitingDestinations set of network nodes to replace the sender path with (they will be appended to the sender path)
     * @param updatesCount increased by the number of paths that replaced the "sender"
     * @return true, if the sender was removed
     */
    private boolean replaceWaitingPaths(List<NetworkNode> senderPath, Set<NetworkNode> notWaitingDestinations, AtomicInteger updatesCount) {
        if (!removeWaitingPath(senderPath))
            return false;
        for (NetworkNode node : notWaitingDestinations) {
            List<NetworkNode> newPath = new ArrayList<NetworkNode>(senderPath);
            newPath.add(node);
            addWaitingPath(newPath);
        }
        
        if (updatesCount != null)
            updatesCount.getAndAdd(notWaitingDestinations.size());
        
        return true;
    }

    /**
     * Update waiting paths list when a message arrives
     * <ul>
     * <li>Every skipWaiting node in the navigation path is replaced by its notWaitingList.</li>
     * <li>The last navigation path element (the reply) in then replaced notWaitingList.
     *     If the sender path is not found in currently waiting list, return <tt>false</tt>
     *     (i.e. the message remains uncommited)</li>
     * </ul>
     * 
     * @param msg the message to process
     * @param updatesCount increased by the number of paths that have been replaced
     * @return <tt>true</tt> if the message is commited (fully processed)
     */
    private boolean updateWaitingPaths(TReplyMessage msg, AtomicInteger updatesCount) {
        Iterator<NavigationElement> pathIterator = msg.getPathElements();
        ArrayList<NetworkNode> path = new ArrayList<NetworkNode>();
        
        while (pathIterator.hasNext()) {
            NavigationElement element = pathIterator.next();
            path.add(element.getSender());
            
            if (element.isSkipping())
                replaceWaitingPaths(path, element.getNotWaitingDestinations(), updatesCount);
            else if (!pathIterator.hasNext())
                if (!replaceWaitingPaths(path, element.getNotWaitingDestinations(), updatesCount))
                    return false;
        }
        
        // Message is fully processed, add message to the answer
        messages.add(msg);
        
        return true;
    }


    //****************** Message receiving ******************//

    /**
     * The {@link Receiver} interface method.
     * This receiver accepts (i.e. returns <tt>true</tt>) only reply messages (with specified class) and only if it is not finished yet.
     * This method is called by {@link MessageDispatcher} when new message arrives.
     *
     * @param msg the message offered for acceptance
     * @param allowSuperclass this receiver only receives message if this parameter is <tt>false</tt>
     * @return <tt>true</tt> if the message is accepted by this receiver
     */
    @Override
    public boolean acceptMessage(Message msg, boolean allowSuperclass) {
        // Don't accept message if allowing super class messages, the receiver has finished or the message has wrong class
        if (allowSuperclass || isFinished() || !replyClass.isInstance(msg))
            return false;
        
        TReplyMessage replyMessage = replyClass.cast(msg);
        
        synchronized (messages) {
            AtomicInteger updatesCount = new AtomicInteger(0);
            
            // Update waiting nodes
            if (!updateWaitingPaths(replyMessage, updatesCount)) {
                uncommitedMessages.add(replyMessage);
                if (log.isLoggable(Level.FINER))
                    log.finer(toString());
                return true;
            }
            
            // Update uncommited messages
            while (updatesCount.get() > 0) {
                updatesCount.set(0);
                for (Iterator<TReplyMessage> iterator = uncommitedMessages.iterator(); iterator.hasNext();) {
                    if (updateWaitingPaths(iterator.next(), updatesCount))
                        iterator.remove();
                }
            }
            
            // Wake waiting thread if last awaiting message received and deregister receiver
            if (isFinished())
                messages.notifyAll();
        }
        if (log.isLoggable(Level.FINER))
            log.finer(toString());
        return true;
    }


    //****************** Message waiting ******************//

    /**
     * Returns all reply messages gathered by this reply receiver so far.
     * If the timeout is not zero, the returned list can be incomplete -
     * the {@link #isFinished} or {@link #getRemainingCount} methods should be called
     * in order to check the completeness.
     * 
     * @param timeout number of miliseconds to wait for replies arrival. If zero is specified,
     *   this method will wait until all reply messages arrive (or forever if something bad happens).
     * @return all reply messages gathered by this reply receiver so far
     * @throws InterruptedException if the thread was interrupted while waiting for message arrival
     */
    public List<TReplyMessage> getReplies(long timeout) throws InterruptedException {
        synchronized (messages) {
            if (!isFinished())
                // Wait reply messages arrival
                messages.wait(timeout);
        }

        if (registeredToList != null)
            registeredToList.deregisterReceiver(this);

        // Messages retrieved so far (might not be all of them, if timeout specified)
        return Collections.unmodifiableList(messages);
    }

    /**
     * Returns all reply messages gathered by this reply receiver.
     * This method will wait until all reply messages arrive (or forever if something bad happens).
     *
     * @return all reply messages gathered by this reply receiver
     * @throws InterruptedException if the thread was interrupted while waiting for message arrival
     */
    public List<TReplyMessage> getReplies() throws InterruptedException {
        return getReplies(0);
    }

    /**
     * Returns the first received reply message.
     * This method will wait until all reply messages arrive (or forever if something bad happens).
     * @return the first received reply message
     */
    public TReplyMessage getFirstReply() {
        while (true)
            try {
                return getReplies(0).get(0);
            } catch (InterruptedException e) {
            } catch (IndexOutOfBoundsException e) {
                log.warning(toString()+"\n\t"+e.toString());
                return getFirstReply();
            }
    }


    //****************** String representation ******************//

    /**
     * Returns a string representation of this reply receiver.
     * @return a string representation of this reply receiver
     */
    @Override
    public String toString() {
        StringBuffer strBuf = new StringBuffer("ReplyReceiver: ");
        strBuf.append("\tProcessed messages: \n");
        for (TReplyMessage msg : messages)
            strBuf.append("\t\t"+msg.navigationPath+"\n");
        strBuf.append("\tUncommited messages: \n");
        for (TReplyMessage msg : uncommitedMessages)
            strBuf.append("\t\t"+msg.navigationPath+"\n");
        strBuf.append("\tWaiting nodes: "+waitingPaths);
        return strBuf.toString();
    }

}