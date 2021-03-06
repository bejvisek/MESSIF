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
package messif.netbucket;

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import messif.buckets.BucketDispatcher;
import messif.buckets.BucketErrorCode;
import messif.buckets.BucketStorageException;
import messif.buckets.LocalBucket;
import messif.objects.LocalAbstractObject;
import messif.objects.UniqueID;
import messif.objects.keys.AbstractObjectKey;
import messif.objects.util.AbstractObjectList;


/**
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class BucketManipulationRequestMessage extends BucketRequestMessage<BucketManipulationReplyMessage> {
    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;

    /** Logger */
    private static final Logger log = Logger.getLogger("netnode.creator");
    
    //****************** Attributes ******************//

    private final LocalAbstractObject object;
    private final AbstractObjectList<LocalAbstractObject> objects;
    private final UniqueID objectID;
    private final String objectLocator;
    private final AbstractObjectKey objectKey;
    private final boolean deleteObject;
    

    //****************** Constructors ******************//
    
    /**
     * Creates a new instance of BucketManipulationRequestMessage that requests addition of object to a remote bucket
     */
    public BucketManipulationRequestMessage(LocalAbstractObject object, int remoteBucketID) {
        super(remoteBucketID);
        this.object = object;
        this.objects = null;
        this.objectID = null;
        this.objectLocator = null;
        this.objectKey = null;
        this.deleteObject = false;
    }

    /**
     * Creates a new instance of BucketManipulationRequestMessage that requests addition of list of objects to a remote bucket
     */
    public BucketManipulationRequestMessage(Iterator<? extends LocalAbstractObject> objects, int remoteBucketID) {
        super(remoteBucketID);
        this.object = null;
        this.objects = new AbstractObjectList<LocalAbstractObject>();
        this.objectID = null;
        this.objectLocator = null;
        this.objectKey = null;
        this.deleteObject = false;

        while (objects.hasNext())
            this.objects.add(objects.next());
    }

    /**
     * Creates a new instance of BucketManipulationRequestMessage that requests retrieval of object from a remote bucket
     */
    public BucketManipulationRequestMessage(UniqueID remoteObjectID, int remoteBucketID) {
        this(remoteObjectID, remoteBucketID, false);
    }
    
    /**
     * Creates a new instance of BucketManipulationRequestMessage that requests retrieval/deletion of object from a remote bucket
     */
    public BucketManipulationRequestMessage(UniqueID remoteObjectID, int remoteBucketID, boolean deleteObject) {
        super(remoteBucketID);
        this.object = null;
        this.objects = null;
        this.objectID = remoteObjectID;
        this.objectLocator = null;
        this.objectKey = null;
        this.deleteObject = deleteObject;
    }

    /**
     * Creates a new instance of BucketManipulationRequestMessage that requests retrieval of object from a remote bucket
     */
    public BucketManipulationRequestMessage(String remoteObjectLocator, int remoteBucketID) {
        this(remoteObjectLocator, remoteBucketID, false);
    }

    /**
     * Creates a new instance of BucketManipulationRequestMessage that requests retrieval of object from a remote bucket
     */
    public BucketManipulationRequestMessage(String remoteObjectLocator, int remoteBucketID, boolean deleteObject) {
        super(remoteBucketID);
        this.object = null;
        this.objects = null;
        this.objectID = null;
        this.objectLocator = remoteObjectLocator;
        this.objectKey = null;
        this.deleteObject = deleteObject;
    }
    
    /**
     * Creates a new instance of BucketManipulationRequestMessage that requests retrieval of object from a remote bucket
     */
    public BucketManipulationRequestMessage(AbstractObjectKey remoteObjectKey, int remoteBucketID) {
        super(remoteBucketID);
        this.object = null;
        this.objects = null;
        this.objectID = null;
        this.objectLocator = null;
        this.objectKey = remoteObjectKey;
        this.deleteObject = false;
    }
    
    /**
     * Creates a new instance of BucketManipulationRequestMessage that requests retrieval of all objects from a remote bucket
     */
    public BucketManipulationRequestMessage(int remoteBucketID) {
        super(remoteBucketID);
        this.object = null;
        this.objects = null;
        this.objectID = null;
        this.deleteObject = false;
        this.objectLocator = null;
        this.objectKey = null;
    }


    //****************** Perform operation ******************//
    
    @Override
    public BucketManipulationReplyMessage execute(BucketDispatcher bucketDispatcher) throws BucketStorageException {
        // Get bucket from bucket dispatcher
        LocalBucket bucket = bucketDispatcher.getBucket(bucketID);

        if (object != null) {
            log.log(Level.INFO, "Adding {0} from {1} into {2}", new Object[]{object, getSender(), bucket});
            bucket.addObject(object);
            return new BucketManipulationReplyMessage(this, bucket.isSoftCapacityExceeded() ? BucketErrorCode.SOFTCAPACITY_EXCEEDED : BucketErrorCode.OBJECT_INSERTED);
        } else if (objects != null) {
            log.log(Level.INFO, "Adding set of {0} objects from {1} into {2}", new Object[]{objects.size(), getSender(), bucket});
            bucket.addObjects(objects);
            return new BucketManipulationReplyMessage(this, BucketErrorCode.OBJECT_INSERTED);
        } else if (deleteObject) {
            if (objectID != null) {
                log.log(Level.INFO, "Deleting from {0} object {1} from {2}", new Object[]{getSender(), objectID, bucket});
                return new BucketManipulationReplyMessage(this, bucket.deleteObject(objectID), true);
            } else {
                log.log(Level.INFO, "Deleting from {0} object with locator {1} from {2}", new Object[]{getSender(), objectLocator, bucket});
                bucket.deleteObject(objectLocator, 0);
                return new BucketManipulationReplyMessage(this, null, true);
            }
        } else if (objectID != null) {
            log.log(Level.INFO, "Returning object {0} from {1} to {2}", new Object[]{objectID, bucket, getSender()});
            return new BucketManipulationReplyMessage(this, bucket.getObject(objectID));
        } else if (objectLocator != null) {
            log.log(Level.INFO, "Returning object with locator {0} from {1} to {2}", new Object[]{objectLocator, bucket, getSender()});
            return new BucketManipulationReplyMessage(this, bucket.getObject(objectLocator));
        } else if (objectKey != null) {
            log.log(Level.INFO, "Returning object with key {0} from {1} to {2}", new Object[]{objectKey, bucket, getSender()});
            return new BucketManipulationReplyMessage(this, bucket.getObject(objectKey));
        } else {
            log.log(Level.INFO, "Returning all objects from {0} to {1}", new Object[]{bucket, getSender()});
            return new BucketManipulationReplyMessage(this, bucket.getAllObjects());
        }
    }

    @Override
    public Class<BucketManipulationReplyMessage> replyMessageClass() {
        return BucketManipulationReplyMessage.class;
    }
                
}
