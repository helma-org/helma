/*
* Helma License Notice
*
* The contents of this file are subject to the Helma License
* Version 2.0 (the "License"). You may not use this file except in
* compliance with the License. A copy of the License is available at
* http://adele.helma.org/download/helma/license.txt
*
* Copyright 1998-2003 Helma Software. All Rights Reserved.
*
* $RCSfile$
* $Author$
* $Revision$
* $Date$
*/

package helma.scripting.rhino.debug;

import org.mozilla.javascript.tools.debugger.Main;
import org.mozilla.javascript.tools.debugger.SwingGui;
import org.mozilla.javascript.tools.debugger.Dim;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.debug.DebuggableScript;

import javax.swing.tree.*;
import javax.swing.*;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import java.util.Enumeration;
import java.util.HashMap;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.KeyAdapter;
import java.awt.*;

import helma.util.StringUtils;


public class HelmaDebugger extends Dim implements TreeSelectionListener {

    DebugGui gui;
    JTree tree;
    JList list;
    DebuggerTreeNode treeRoot;
    DefaultTreeModel treeModel;
    HashMap treeNodes = new HashMap();
    HashMap scriptNames = new HashMap();


    public HelmaDebugger(String title) {
        gui = new DebugGui(this, title);
        gui.pack();
        gui.setVisible(true);
    }

    public void handleCompilationDone(Context cx, DebuggableScript fnOrScript,
                                      String source) {
        String sourceName = fnOrScript.getSourceName();
        // FileWindow w = (FileWindow) fileWindows.get(sourceName);
        /* super.(cx, fnOrScript, source);
        if (!treeNodes.containsKey(sourceName)) {
            createTreeNode(sourceName);
        } */
        /* if (w != null) {
            // renew existing file window
            int position = w.textArea.getCaretPosition();
            // System.err.println("         VISIBLE: " + point);
            // w.sourceInfo.removeAllBreakpoints();
            w.sourceInfo = (SourceInfo) sourceNames.get(sourceName);
            w.updateText();
            w.textArea.setCaretPosition(position);
        } */
    }

    void createTreeNode(String sourceName, Dim.SourceInfo sourceInfo) {
        String[] path = StringUtils.split(sourceName, ":/\\");
        DebuggerTreeNode node = treeRoot;
        DebuggerTreeNode newNode = null;
        for (int i = path.length-2; i < path.length; i++) {
            DebuggerTreeNode n = node.get(path[i]);
            if (n == null) {
                n = new DebuggerTreeNode(path[i]);
                node.add(n);
                if (newNode == null) newNode = n;
            }
            node = n;
        }
        treeNodes.put(sourceName, node);
        scriptNames.put(node, sourceName);
        if (newNode != null) {
            SwingUtilities.invokeLater(new NodeInserter(newNode));
        }
    }

    void openScript(TreePath path) {
        if (path == null)
            return;
        Object node = path.getLastPathComponent();
        if (node == null)
            return;
        String sourceName = (String) scriptNames.get(node);
        if (sourceName == null)
            return;
        SourceInfo sourceInfo = sourceInfo(sourceName);
        gui.showSourceText(sourceInfo);
        // display functions for opened script file
        /*Vector functions = new Vector();
        Iterator it = functionNames.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            ScriptItem si = (ScriptItem) entry.getValue();
            if (scriptName.equals(si.getSourceInfo().getUrl())) {
                functions.add(entry.getKey());
            }
        }
        Collections.sort(functions);
        list.setListData(functions); */
    }

    void openFunction(String function) {
        if (function == null)
            return;
        /* ScriptItem item = (ScriptItem) functionNames.get(function);
        if (item != null) {
            SourceInfo si = item.getSourceInfo();
            String url = si.getUrl();
            int lineNumber = item.getFirstLine();
            FileWindow w = getFileWindow(url);
            if (w == null) {
                CreateFileWindow.action(this, si, lineNumber).run();
                w = getFileWindow(url);
                w.setPosition(-1);
            }
            int start = w.getPosition(lineNumber - 1);
            int end = w.getPosition(lineNumber) - 1;
            w.textArea.select(start);
            w.textArea.setCaretPosition(start);
            w.textArea.moveCaretPosition(end);
            try {
                if (w.isIcon())
                    w.setMaximum(true);
                w.show();
                requestFocus();
                w.requestFocus();
                w.textArea.requestFocus();
            } catch (Exception exc) {
            }
        } */
    }



   public void valueChanged(TreeSelectionEvent e) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)
                tree.getLastSelectedPathComponent();

        if (node == null) return;

        Object script = scriptNames.get(node);
        if (script != null) {
            // openScript(script);
        }
    }

    class DebuggerTreeNode extends DefaultMutableTreeNode {

        public DebuggerTreeNode(Object obj) {
            super(obj);
        }

        public DebuggerTreeNode get(String name) {
            Enumeration children = this.children();
            while (children.hasMoreElements()) {
                DebuggerTreeNode node = (DebuggerTreeNode) children.nextElement();
                if (node != null && name.equals(node.getUserObject()))
                    return node;
            }
            return null;
        }
    }

    class NodeInserter implements Runnable {
        MutableTreeNode node;

        NodeInserter(MutableTreeNode node) {
            this.node = node;
        }

        public void run() {
            MutableTreeNode parent = (MutableTreeNode) node.getParent();
            if (parent == treeRoot && treeRoot.getChildCount() == 1) {
                tree.makeVisible(new TreePath(new Object[]{parent, node}));
            }
            treeModel.insertNodeInto(node, parent, parent.getIndex(node));
        }
    }

    class DebugGui extends SwingGui {
        public DebugGui(Dim dim, String title) {
            super(dim, title);
            Container contentPane = getContentPane();
            Component main = contentPane.getComponent(1);
            contentPane.remove(main);

            treeRoot = new DebuggerTreeNode(title);
            tree = new JTree(treeRoot);
            treeModel = new DefaultTreeModel(treeRoot);
            tree.setModel(treeModel);
            tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
            tree.addTreeSelectionListener(HelmaDebugger.this);
            // tree.setRootVisible(false);
            // track double clicks
            tree.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent evt) {
                    openScript(tree.getSelectionPath());
                }
            });
            // track enter key
            tree.addKeyListener(new KeyAdapter() {
                public void keyPressed(KeyEvent evt) {
                    if (evt.getKeyCode() == KeyEvent.VK_ENTER)
                        openScript(tree.getSelectionPath());
                }
            });
            JScrollPane treeScroller = new JScrollPane(tree);
            treeScroller.setPreferredSize(new Dimension(180, 300));

            list = new JList();
            // no bold font lists for me, thanks
            list.setFont(list.getFont().deriveFont(Font.PLAIN));
            list.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent evt) {
                    openFunction((String) list.getSelectedValue());
                }
            });
            list.addKeyListener(new KeyAdapter() {
                public void keyPressed(KeyEvent evt) {
                    if (evt.getKeyCode() == KeyEvent.VK_ENTER)
                        openFunction((String) list.getSelectedValue());
                }
            });
            JScrollPane listScroller = new JScrollPane(list);
            listScroller.setPreferredSize(new Dimension(180, 200));

            JSplitPane split1 = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
            split1.setTopComponent(treeScroller);
            split1.setBottomComponent(listScroller);
            split1.setOneTouchExpandable(true);
            split1.setResizeWeight(0.66);

            JSplitPane split2 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
            split2.setLeftComponent(split1);
            split2.setRightComponent(main);
            split2.setOneTouchExpandable(true);
            contentPane.add(split2, BorderLayout.CENTER);
        }

        public void updateSourceText(Dim.SourceInfo sourceInfo) {
            // super.updateSourceText(sourceInfo);
            String filename = sourceInfo.url();
            if (!treeNodes.containsKey(filename)) {
                createTreeNode(filename, sourceInfo);
            }
            // System.err.println("UPDATE SOURCE TEXT CALLED: " + sourceInfo.url());
        }

        public void showSourceText(Dim.SourceInfo sourceInfo) {
            super.updateSourceText(sourceInfo);
        }
    }
}

