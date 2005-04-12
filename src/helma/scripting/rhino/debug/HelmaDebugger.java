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

import org.mozilla.javascript.Context;
import org.mozilla.javascript.debug.DebuggableScript;

import javax.swing.*;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.*;

import helma.util.StringUtils;

public class HelmaDebugger extends Main implements TreeSelectionListener {

    JTree tree;
    JList list;
    DebuggerTreeNode treeRoot;
    DefaultTreeModel treeModel;
    HashMap treeNodes = new HashMap();
    HashMap scriptNames = new HashMap();

    public HelmaDebugger(String name) {
        super(name);
        Container contentPane = getContentPane();
        Component main = contentPane.getComponent(1);
        contentPane.remove(main);

        treeRoot = new DebuggerTreeNode(name);
        tree = new JTree(treeRoot);
        treeModel = new DefaultTreeModel(treeRoot);
        tree.setModel(treeModel);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.addTreeSelectionListener(this);
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
        Main.setResizeWeight(split1, 0.66);

        JSplitPane split2 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split2.setLeftComponent(split1);
        split2.setRightComponent(main);
        split2.setOneTouchExpandable(true);
        contentPane.add(split2, BorderLayout.CENTER);
    }

    public void setVisible(boolean b) {
        super.setVisible(b);
        // hide console window
        console.hide();
    }

    public void handleCompilationDone(Context cx, DebuggableScript fnOrScript,
                                      String source) {
        String sourceName = fnOrScript.getSourceName();
        FileWindow w = (FileWindow) fileWindows.get(sourceName);
        super.handleCompilationDone(cx, fnOrScript, source);
        if (!treeNodes.containsKey(sourceName)) {
            createTreeNode(sourceName);
        }
        if (w != null) {
            // renew existing file window
            int position = w.textArea.getCaretPosition();
            // System.err.println("         VISIBLE: " + point);
            // w.sourceInfo.removeAllBreakpoints();
            w.sourceInfo = (SourceInfo) sourceNames.get(sourceName);
            w.updateText();
            w.textArea.setCaretPosition(position);
        }
    }

    void createTreeNode(String sourceName) {
        String[] path = StringUtils.split(sourceName, ":/\\");
        DebuggerTreeNode node = treeRoot;
        DebuggerTreeNode newNode = null;
        for (int i = 1; i < path.length; i++) {
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
        String scriptName = (String) scriptNames.get(node);
        if (scriptName == null)
            return;
        JInternalFrame w = (JInternalFrame) fileWindows.get(scriptName);
        if (w != null) {
            try {
                if (w.isIcon())
                    w.setMaximum(true);
                w.show();
                w.setSelected(true);
            } catch (Exception exc) {
            }
        } else {
            SourceInfo si = (SourceInfo) sourceNames.get(scriptName);
            if (si == null) {
                System.out.println("debugger error: Couldn't find source: " + scriptName);
            }
            swingInvoke(CreateFileWindow.action(this, si, -1));
        }
        // display functions for opened script file
        Vector functions = new Vector();
        Iterator it = functionNames.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            ScriptItem si = (ScriptItem) entry.getValue();
            if (scriptName.equals(si.getSourceInfo().getUrl())) {
                functions.add(entry.getKey());
            }
        }
        Collections.sort(functions);
        list.setListData(functions);
    }

    void openFunction(String function) {
        if (function == null)
            return;
        ScriptItem item = (ScriptItem) functionNames.get(function);
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
        }
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

}

