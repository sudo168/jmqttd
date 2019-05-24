package net.ewant.jmqttd.server.mqtt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;

public class TopicTrie {
	
	private static final String SYSTEM_LEADING_CHAR = "$";
	private static final String SINGLE_LEVEL_CHAR = "+";
	private static final String MULTI_LEVEL_CHAR = "#";
	private static final String LEVEL_SPLITER_CHAR = "/";

    private Node root;

    public TopicTrie(){
        root = new Node("root");
    }

    /**
     * 查询与主题匹配的所有节点，通常在有消息发布到指定主题时，查询该主题消息需要下发给哪些节点
     * @param topicName 主题名不能包含 + #
     * @return
     */
    public List<TopicMapping> search(String topicName){
        if(topicName == null){
            return null;
        }
        
        List<TopicMapping> mappingList = new ArrayList<TopicMapping>();
        String[] topics = topicName.split(LEVEL_SPLITER_CHAR);

        doSearch(mappingList, root, topics, 0);

        return mappingList;
    }

    private void doSearch(List<TopicMapping> result, Node current, String[] topics, int index) {

        if(current != null){
            Map<String, Node> children;
            if (index < topics.length && !(children = current.getChildren()).isEmpty()) {
            	// A subscription to “#” will not receive any messages published to a topic beginning with a $
            	boolean specialTopic = index == 0 && topics[index].startsWith(SYSTEM_LEADING_CHAR);
                Node anyNode = children.get(MULTI_LEVEL_CHAR);
                if(anyNode != null && !specialTopic){
                    if(anyNode.isEnd()){
                    	result.add(anyNode.getData());
                    }
                }

                // A subscription to “+/monitor/Clients” will not receive any messages published to “$SYS/monitor/Clients”
                Node node = children.get(topics[index]);
                if(!specialTopic){
                	if(node == null){
                        node = children.get(SINGLE_LEVEL_CHAR);
                    }else{
                        // two match branch
                        Node levelNode = children.get(SINGLE_LEVEL_CHAR);
                        doSearch(result, levelNode, topics, index + 1);
                    }
                }
                doSearch(result, node, topics, index + 1);
            }else if(current.isEnd() && index == topics.length){
            	result.add(current.getData());
            }
        }
    }

    /**
     * 只查询指定主题的节点
     * @param topicName 主题名可包含 + #
     * @return
     */
    private Node searchNode(String topicName){
        if(topicName == null){
            return null;
        }
        String[] topics = topicName.split(LEVEL_SPLITER_CHAR);
        Node current = root;
        Map<String, Node> children;
        for (int i = 0; i < topics.length && !(children = current.getChildren()).isEmpty(); i++) {
            Node node = children.get(topics[i]);
            if(node == null){
                break;
            }else if(i + 1 == topics.length){
                return node;
            }
            current = node;
        }

        return null;
    }

    /**
     * 插入节点，通常在订阅的时候使用
     * @param topicName
     * @param qos
     * @param clientId
     * @return
     */
    public int insert(String topicName, int qos, String clientId){
        if(topicName == null || clientId == null){
            return 0;
        }
        Node node = this.searchNode(topicName);
        if (node != null){
            TopicMapping topicMapping = node.getData();
            if(topicMapping == null){
                topicMapping = new TopicMapping();
                topicMapping.setName(topicName);
                node.setData(topicMapping);
            }
            topicMapping.getSubscribers().put(clientId, qos);
            node.setEnd(true);
            return 1;
        }
        String[] topics = topicName.split(LEVEL_SPLITER_CHAR);
        Node parent = root;
        for (int i = 0; i < topics.length; i++) {
            Node child = createNode(parent, topics[i]);

            if(i + 1 == topics.length){
                TopicMapping topicMapping = new TopicMapping();
                topicMapping.setName(topicName);
                topicMapping.getSubscribers().put(clientId, qos);

                child.setData(topicMapping);
                child.setEnd(true);
            }
            parent = child;
        }
        return 1;
    }

    private Node createNode(Node parent, String name){
        Node child = parent.getChild(name);
        if(child == null){
            child = new Node(name);
            parent.addChild(child);
            child.setParent(parent);
        }
        return child;
    }

    /**
     * 删除节点，通常在取消订阅时使用
     * @param topicName
     * @param clientId
     * @return
     */
    public TopicMapping remove(String topicName, String clientId){
        Node node = this.searchNode(topicName);
        if(node != null){
            TopicMapping topicMapping = node.getData();
            if(topicMapping != null){
                topicMapping.getSubscribers().remove(clientId);
                if(topicMapping.getSubscribers().isEmpty()){
                    node.remove();
                }
                return topicMapping;
            }
        }
        return null;
    }

    public class Node {

    	/**
    	 * 树节点名，这里即是主题
    	 */
        private String name;
        /**
         * 主题订阅信息
         */
        private TopicMapping data;
        /**
         * 父节点
         */
        private Node parent;
        /**
         * end 为true时，表示从根节点到此为一个有效订阅主题
         */
        private boolean end;
        /**
         * 叶子节点
         */
        private Map<String,Node> children = new HashMap<String, Node>();

        public Node(String name){
            this.name = name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public TopicMapping getData() {
            return data;
        }

        public void setData(TopicMapping data) {
            this.data = data;
        }

        public void setParent(Node parent) {
            this.parent = parent;
        }

        public Node getChild(String name) {
            return children.get(name);
        }

        public Node addChild(Node node) {
           return children.put(node.name, node);
        }

        public Node removeChild(Node node) {
           return children.remove(node.name);
        }

        public Map<String, Node> getChildren() {
            return children;
        }

        public boolean isEnd() {
            return end;
        }

        public void setEnd(boolean end) {
            this.end = end;
        }

        public void remove() {
            if(this.parent != null){
                this.parent.removeChild(this);
                if(this.parent.getChildren().isEmpty() && !this.parent.isEnd()){
                    this.parent.remove();
                }
            }
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        TopicTrie trie = new TopicTrie();
        trie.insert("t/a",1, "111");
        trie.insert("t/#",0, "222");
        trie.insert("t/+/x",1, "333");
        trie.insert("t/+/y",2, "444");
        trie.insert("t/x/y",1, "555");
        trie.insert("t/+",1, "555");
        trie.insert("tp/+",1, "555");

        //trie.remove("t/+/x", "333");
        //trie.remove("t/+/y", "444");
        //trie.remove("tp/+", "555");


        System.out.println(JSON.toJSON(trie.root));

        List<TopicMapping> nodes = trie.search("t/x");

        System.out.println("======================");
        System.out.println(JSON.toJSON(nodes));
    }
}
