package main.compression;

import java.util.*;
import java.io.ByteArrayOutputStream; // Optional
import java.lang.reflect.Array;

/**
 * Huffman instances provide reusable Huffman Encoding Maps for
 * compressing and decompressing text corpi with comparable
 * distributions of characters.
 */
public class Huffman {
    
    // -----------------------------------------------
    // Construction
    // -----------------------------------------------

    private HuffNode trieRoot;
    // TreeMap chosen here just to make debugging easier
    private TreeMap<Character, String> encodingMap;
    // Character that represents the end of a compressed transmission
    private static final char ETB_CHAR = 23;
    
    /**
     * Creates the Huffman Trie and Encoding Map using the character
     * distributions in the given text corpus
     * 
     * @param corpus A String representing a message / document corpus
     *        with distributions over characters that are implicitly used
     *        throughout the methods that follow. Note: this corpus ONLY
     *        establishes the Encoding Map; later compressed corpi may
     *        differ.
     */
    public Huffman (String corpus) {
        HashMap<Character, Integer> distMap = getFrequencyDistribution(corpus);
        PriorityQueue<HuffNode> queue = new PriorityQueue<>();

        for (Map.Entry<Character, Integer> entry : distMap.entrySet()){
            queue.add(new HuffNode(entry.getKey(), entry.getValue()));
        }

        while (queue.size() > 1){
            HuffNode node = queue.poll();
            HuffNode node2 = queue.poll();

            HuffNode parent = new HuffNode(ETB_CHAR, node.count + node2.count);
            parent.zeroChild = node;
            parent.oneChild = node2;
            queue.add(parent);
        }

        trieRoot = queue.poll();
        TreeMap<Character, String> storageMap = new TreeMap<Character, String>();
        encodingMap = getEncodeMap(storageMap, trieRoot, "");
    }

    /**
     * Gets the Frequency Distribution using the given Characters in the corpus
     * @param corpus A String with the message
     * @return A HashMap indicating each character as the Key and the
     * number of appearances in the corpus as the value
     */
    public HashMap<Character, Integer> getFrequencyDistribution(String corpus){
        HashMap<Character, Integer> map = new HashMap<Character, Integer>();
        for (int i = 0; i < corpus.length(); i++) {
            char c = corpus.charAt(i);
            Integer val = map.get(c);
            if (val != null) {
                map.put(c, map.get(c) + 1);
            }
            else {
               map.put(c, 1);
           }
        }
        map.put(ETB_CHAR, 1);
        return map;
    }
    
    /**
     * Recurses through the Tree to form the final encode map
     * @param map An initially empty map to store the data on each recursion
     * @param node The node of the tree that is currently being recursed on. Starts
     * at the root.
     * @param string The 0 and 1 combination stored for a certain node 
     * @return The final TreeMap of characters and conversions
     */
    public TreeMap<Character, String> getEncodeMap(TreeMap<Character, String> map, HuffNode node, String string){
        if (!node.isLeaf()){
            getEncodeMap(map, node.zeroChild, string + 0);
            getEncodeMap(map, node.oneChild, string + 1);

        }
        if (node.isLeaf()){
            map.put(node.character, string);
        }
        return map;
    }

    // -----------------------------------------------
    // Compression
    // -----------------------------------------------
    
    /**
     * Compresses the given String message / text corpus into its Huffman coded
     * bitstring, as represented by an array of bytes. Uses the encodingMap
     * field generated during construction for this purpose.
     * 
     * @param message String representing the corpus to compress.
     * @return {@code byte[]} representing the compressed corpus with the
     *         Huffman coded bytecode. Formatted as:
     *         (1) the bitstring containing the message itself, (2) possible
     *         0-padding on the final byte.
     */
    public byte[] compress (String message) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        String fin = "";
        message += ETB_CHAR;
        for (char ch : message.toCharArray()) {
            int toWrite = Integer.parseInt(encodingMap.get(ch));
            fin += toWrite;
          }
        String chunk = "";
        while(fin.length() > 0){
            chunk = formChunk(fin);
            if (fin.length() >= 8){
                fin = fin.substring(8);
            }else{
                fin = "";
            }
            output.write((byte) Integer.parseInt(chunk, 2));
        }
        return output.toByteArray();
    }

    /**
     * Separates out every 8 numbers
     * @param message The String being separated
     * @return The next 8 Character in the String
     */
    public String formChunk (String message){
        String chunk = "";
        for (int i = 0; i < 8; i++) {
            if (i < message.length()){
                chunk += message.charAt(i);
            }else{
                chunk += 0;
            }
        }
        return chunk;
    }
    
    
    // -----------------------------------------------
    // Decompression
    // -----------------------------------------------
    
    /**
     * Decompresses the given compressed array of bytes into their original,
     * String representation. Uses the trieRoot field (the Huffman Trie) that
     * generated the compressed message during decoding.
     * 
     * @param compressedMsg {@code byte[]} representing the compressed corpus with the
     *        Huffman coded bytecode. Formatted as:
     *        (1) the bitstring containing the message itself, (2) possible
     *        0-padding on the final byte.
     * @return Decompressed String representation of the compressed bytecode message.
     */
    public String decompress (byte[] compressedMsg) {
        String fin = "";
        String currentString = "";

        for(int i = 0; i < compressedMsg.length; i++)
        {
            int byteCode = compressedMsg[i];
            if (byteCode < 0){
                int temp = 128 - Math.abs(byteCode);
                byteCode = 128 + temp;
            }
            String convertedString = Integer.toBinaryString(byteCode);
            while (convertedString.length() != 8){
                convertedString = "0" + convertedString;
            }
            byteCode = Integer.parseInt(convertedString);
            currentString += convertedString;
        }
        return decode(trieRoot, 0, currentString, fin);
    }
    
    /**
     * Converts the bytes back into a String
     * @param node The current node
     * @param pos The depth of traversal through the nodes
     * @param s The string being decoded
     * @param fin The final string
     * @return A String containing the original decoded String from the bytes
     */
    public String decode(HuffNode node, int pos, String cur, String fin){
        if (pos == cur.length()){
            return fin;
        }
        char c = cur.charAt(pos);
        if (c == '0' && !node.isLeaf()){
            return decode(node.zeroChild, pos + 1, cur, fin);
        }
        if (c == '1' && !node.isLeaf()){
            return decode(node.oneChild, pos + 1, cur, fin);
        }
        if (node.isLeaf()){
            if (node.character != ETB_CHAR){
                fin += node.character;
            }
            if (node.character == ETB_CHAR){
                return fin;
            }            
        }
        return decode(trieRoot, pos, cur, fin);
    }
    
    // -----------------------------------------------
    // Huffman Trie
    // -----------------------------------------------
    
    /**
     * Huffman Trie Node class used in construction of the Huffman Trie.
     * Each node is a binary (having at most a left (0) and right (1) child), contains
     * a character field that it represents, and a count field that holds the 
     * number of times the node's character (or those in its subtrees) appear 
     * in the corpus.
     */
    private static class HuffNode implements Comparable<HuffNode> {
        
        HuffNode zeroChild, oneChild;
        char character;
        int count;
        
        HuffNode (char character, int count) {
            this.count = count;
            this.character = character;
        }
        
        public boolean isLeaf () {
            return this.zeroChild == null && this.oneChild == null;
        }
        
        public int compareTo (HuffNode other) {
            int pri = this.count - other.count;
            if (pri == 0){
                return Character.compare(this.character, other.character);
            }
            return pri;
        }
        
    }

}
