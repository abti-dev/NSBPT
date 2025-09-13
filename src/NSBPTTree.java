
import java.util.*;
import java.util.List;

/**
 * @Author: evan
 * @Description: TODO
 * @DateTime: 2025/8/21 14:24
 **/
public class NSBPTTree {
    static int order;
    private final Node root;
    private InsertionRecord insertionRecordHead;

    public NSBPTTree() {
        NSBPTTree.order = 7;
        this.root = new Node();
        root.values = new ArrayList<>(order);
        root.isLeafNode = true;
    }

    public NSBPTTree(int order, String ownerId) {
        NSBPTTree.order = order;
        this.root = new Node();
        root.values = new ArrayList<>(order);
        root.isLeafNode = true;
    }

    //insert方法
    public void insertNewNode(String k, int v) {
        Node targetNode = searchTargetNode(k, this.root);
        if (targetNode.keys.isEmpty()) {
            targetNode.keys.add(k);
            List<Integer> value = new ArrayList<>();
            value.add(v);
            targetNode.values.add(value);
            addToInsertionHead(targetNode, 0);
            return;
        }
        if (!targetNode.isLeafNode) {//应该往左下移
            leftDownMove(k, v, targetNode);
            return;
        }
        int targetPos = Collections.binarySearch(targetNode.keys, k);
        if (targetPos >= 0) {
            targetNode.values.get(targetPos).add(v);//此时是找到了，直接追加即可
        } else {
            targetPos = -(targetPos + 1);//插入位置
            if (targetNode.hasRoom()) {//有房间，直接插入
                targetNode.keys.add(targetPos, k);
                List<Integer> value = new ArrayList<>();
                value.add(v);
                targetNode.values.add(targetPos, value);
                // 添加到插入顺序链表头部
                addToInsertionHead(targetNode, targetPos);
            } else {
                // 检查兄弟节点可用性
                Node availableNext = (targetNode.next != null && targetNode.next.hasRoom()) ? targetNode.next : null;
                Node availablePrev = (targetNode.prev != null && targetNode.prev.hasRoom()) ? targetNode.prev : null;
                if (availableNext != null && availablePrev != null) {
                    // 两个都可用，随机选择
                    if (Math.random() > 0.5) {
                        borrowRoom(targetNode, availablePrev, true, targetPos, k, v);
                        // borrowRoom方法内部会处理插入顺序链表
                    } else {
                        borrowRoom(targetNode, availableNext, false, targetPos, k, v);
                        // borrowRoom方法内部会处理插入顺序链表
                    }
                } else if (availableNext != null) {
                    borrowRoom(targetNode, availableNext, false, targetPos, k, v);
                    // borrowRoom方法内部会处理插入顺序链表
                } else if (availablePrev != null) {
                    borrowRoom(targetNode, availablePrev, true, targetPos, k, v);
                    // borrowRoom方法内部会处理插入顺序链表
                } else {//下推的逻辑。
                    List<Integer> newValue = new ArrayList<>();
                    newValue.add(v);
                    Node prevChild = targetNode.leftChild;

                    for (int i = 0; i < targetNode.keys.size(); i++) {
                        // 构建当前子节点的数据
                        List<List<Integer>> childValues = new ArrayList<>();
                        childValues.add(targetNode.values.get(i));
                        List<String> childKeys = new ArrayList<>();
                        childKeys.add(targetNode.keys.get(i));
                        boolean keyInsertedInThisChild = false;
                        int keyIndexInChild = -1;
                        if (targetPos == 0 && i == 0) {
                            // 特殊情况：插入位置为0，需要更新父节点键
                            targetNode.keys.set(0, k);          // 更新父节点的第一个键
                            childValues.add(0, newValue);       // 在索引0插入新值
                            childKeys.add(0, k);               // 在索引0插入新键
                            keyInsertedInThisChild = true;
                            keyIndexInChild = 0;
                        } else if (i == targetPos - 1) {
                            childValues.add(newValue);
                            childKeys.add(k);
                            keyInsertedInThisChild = true;
                            keyIndexInChild = childKeys.size() - 1;
                        }
                        // 创建子节点
                        Node child = new Node(childValues);
                        child.keys.addAll(childKeys);
                        child.parent = targetNode;

                        // 如果新key被插入到这个子节点中，添加到插入顺序链表头部
                        if (keyInsertedInThisChild) {
                            addToInsertionHead(child, keyIndexInChild);
                        }

                        // 设置在父节点中的位置
                        targetNode.rightChildren.set(i, child);

                        // 维护双向链表
                        child.prev = prevChild;
                        if (prevChild != null) prevChild.next = child;
                        prevChild = child;
                    }

                    // 清理原节点数据
                    targetNode.values = null;
                    targetNode.isLeafNode = false;
                }
            }
        }
    }

    private void leftDownMove(String k, int v, Node targetNode) {
        List<List<Integer>> values = new ArrayList<>();
        List<Integer> value = new ArrayList<>();
        value.add(v);
        values.add(value);
        Node newNode = new Node(values);
        newNode.keys.add(0, k);
        targetNode.leftChild = newNode;
        newNode.parent = targetNode;
        Node brother = targetNode.rightChildren.get(0);
        newNode.next = brother;
        if (brother != null) brother.prev = newNode;
        // 添加到插入顺序链表头部
        addToInsertionHead(newNode, 0);
    }

    private void borrowRoom(Node targetNode, Node brother, boolean isLeft, int targetPos, String k, int v) {
        List<Integer> values = new ArrayList<>();
        values.add(v);
        if (isLeft) {
            String key = targetNode.keys.remove(0);
            List<Integer> borrowedData = targetNode.values.remove(0);
            targetNode.keys.add(targetPos - 1, k);
            targetNode.values.add(targetPos - 1, values);
            brother.values.add(borrowedData);
            brother.keys.add(key);
            Node parent = targetNode.parent;
            int pos = Collections.binarySearch(parent.keys, key);
            parent.keys.set(pos, targetNode.keys.get(0));
            addToInsertionHead(targetNode, targetPos - 1);
        } else {
            String temp = brother.keys.get(0);
            targetNode.keys.add(targetPos, k);
            targetNode.values.add(targetPos, values);
            String key = targetNode.keys.remove(targetNode.keys.size() - 1);
            List<Integer> borrowedData = targetNode.values.remove(targetNode.values.size() - 1);
            brother.values.add(0, borrowedData);
            brother.keys.add(0, key);
            Node parent = targetNode.parent;
            if (targetNode.parent.leftChild == targetNode) {
                parent.keys.set(0, key);
            } else {
                int pos = Collections.binarySearch(parent.keys, temp);
                parent.keys.set(pos, brother.keys.get(0));
            }
            addToInsertionHead(targetNode, targetPos);
        }

    }

    public Node searchTargetNode(String key, Node root) {
        if (root.isLeafNode) return root;
        int pos = Collections.binarySearch(root.keys, key);
        if (pos >= 0) return firstSearchTargetNode(key, root.rightChildren.get(pos));
        else {
            int insertPos = -(pos + 1);
            if (insertPos == 0) return root.leftChild == null ? root : searchTargetNode(key, root.leftChild);
            return searchTargetNode(key, root.rightChildren.get(insertPos - 1));
        }
    }

    public Node searchLeftTargetNode(String key, Node root, int[] nums) {
        nums[0]++;
        if (root.isLeafNode) return root;
        int pos = Collections.binarySearch(root.keys, key);
        if (pos >= 0) return firstSearchTargetNode(key, root.rightChildren.get(pos));    // 找到了完全匹配的key，走对应的右子树
        else {   // 没有找到完全匹配，pos是插入位置的负值减1
            int insertPos = -(pos + 1);
            if (insertPos == 0) return root.leftChild == null ? root : searchLeftTargetNode(key, root.leftChild, nums);
            return searchRightTargetNode(key, root.rightChildren.get(insertPos - 1), nums);
        }
    }

    public Node searchRightTargetNode(String key, Node root, int[] nums) {
        nums[0]++;
        if (root.isLeafNode) return root;
        int pos = myBinarySearch(root.keys, key);
        if (pos >= 0) return firstSearchTargetNode(key, root.rightChildren.get(pos));
        else {
            int insertPos = -(pos + 1);
            if (insertPos == 0) return root.leftChild == null ? root : searchLeftTargetNode(key, root.leftChild, nums);
            return searchRightTargetNode(key, root.rightChildren.get(insertPos - 1), nums);
        }
    }

    public Node firstSearchTargetNode(String key, Node root) {
        if (root.isLeafNode) return root;
        return firstSearchTargetNode(key, root.rightChildren.get(0));
    }


    public int myBinarySearch(List<String> list, String k) {
        int left = 1, right = list.size() - 1;
        int pos = -list.size() - 1;
        while (left <= right) {
            int mid = left + ((right - left) >> 1);
            if (list.get(mid).compareTo(k) == 0) {
                return mid;
            } else if (list.get(mid).compareTo(k) > 0) {
                right = mid - 1;
            } else {
                left = mid + 1;
            }
        }
        return -left - 1;
    }


    /**
     * 递归计算节点高度
     *
     * @param node 当前节点
     * @return 以该节点为根的子树高度
     */
    private int calculateHeight(Node node) {
        if (node == null) {
            return 0;
        }

        if (node.isLeafNode) {
            return 1; // 叶子节点高度为1
        }

        // 内部节点：计算所有子节点中的最大高度
        int maxChildHeight = 0;

        // 检查左子节点
        if (node.leftChild != null) {
            maxChildHeight = Math.max(maxChildHeight, calculateHeight(node.leftChild));
        }

        // 检查所有右子节点
        if (node.rightChildren != null) {
            for (Node rightChild : node.rightChildren) {
                if (rightChild != null) {
                    maxChildHeight = Math.max(maxChildHeight, calculateHeight(rightChild));
                }
            }
        }

        return maxChildHeight + 1; // 当前节点高度 = 子节点最大高度 + 1
    }

    /**
     * 将新插入的key添加到插入顺序链表的头部（头插法）
     *
     * @param targetNode 包含新key的节点
     * @param keyIndex   新key在节点中的索引
     */
    private void addToInsertionHead(Node targetNode, int keyIndex) {
        // 创建一个只包含指针信息的轻量级记录
        InsertionRecord record = new InsertionRecord();
        record.node = targetNode;
        record.keyIndex = keyIndex;

        // 头插
        record.next = insertionRecordHead;
        insertionRecordHead = record;
    }


    class InsertionRecord {
        Node node;           // 指向包含key的节点
        int keyIndex;        // key在节点中的索引
        InsertionRecord next; // 指向下一个插入记录
    }

    class Node {
        private static int order;
        List<String> keys;
        List<List<Integer>> values;
        boolean isLeafNode;
        Node leftChild;
        List<Node> rightChildren;
        Node parent;
        Node prev, next;

        public Node() {
            order = NSBPTTree.order;
            this.keys = new ArrayList<>(order);
            this.values = null;
            this.isLeafNode = false;
            this.leftChild = null;
            this.rightChildren = new ArrayList<>(Collections.nCopies(order, null));
            this.parent = null;
        }

        public Node(List<List<Integer>> values) {
            order = NSBPTTree.order;
            this.keys = new ArrayList<>(order);
            this.values = values;
            this.isLeafNode = true;
            this.leftChild = null;
            this.rightChildren = new ArrayList<>(Collections.nCopies(order, null));
            ;
            this.parent = null;
        }

        public boolean hasRoom() {
            return this.keys.size() < order;
        }
    }
}