/*
 * Created on Jul 10, 2004
 */
package net.starmen.pkhack;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import net.starmen.pkhack.CheckNode;


public class NodeSelectionListener extends MouseAdapter
{
    JTree tree;

    public NodeSelectionListener(JTree tree)
    {
        this.tree = tree;
    }

    public void mouseClicked(MouseEvent e)
    {
        int x = e.getX();
        int y = e.getY();
        int row = tree.getRowForLocation(x, y);
        TreePath path = tree.getPathForRow(row);
        //TreePath path = tree.getSelectionPath();
        if (path != null)
        {
            CheckNode node = (CheckNode) path.getLastPathComponent();
            boolean isSelected = !(node.isSelected());
            node.setSelected(isSelected);
            if (node.getSelectionMode() == CheckNode.DIG_IN_SELECTION)
            {
                if (isSelected)
                {
                    tree.expandPath(path);
                }
                else
                {
                    tree.collapsePath(path);
                }
            }
            if (row != 0)
            {
                CheckNode parent = (CheckNode) node.getParent();
                if (node.isSelected() && !parent.isSelected())
                {
                    parent.setSelected(true);
                    for (int p = 0; p < parent.getChildCount(); p++)
                        if (parent.getChildAt(p) != node)
                            ((CheckNode) parent.getChildAt(p))
                                .setSelected(false);
                }
                unSel: if (!node.isSelected() && parent.isSelected())
                {
                    for (int p = 0; p < parent.getChildCount(); p++)
                        if (((CheckNode) parent.getChildAt(p)).isSelected())
                            break unSel;
                    parent.setSelected(false);
                }
            }
            ((DefaultTreeModel) tree.getModel()).nodeChanged(node);
            // I need revalidate if node is root. but why?
            if (row == 0)
            {
                tree.revalidate();
                tree.repaint();
            }
        }
    }
}