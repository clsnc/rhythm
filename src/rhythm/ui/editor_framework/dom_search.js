/* Iterates through a node, its following siblings (as provided by nextSibFunc), and their 
   children. */
function findMatchingDomNodeAmongSiblings(node, pred, nextSibFunc, firstCheckChildFunc) {
    let currNode = node
    while(currNode) {
        if(pred(currNode)) {
            return currNode
        } else {
            const firstCheckChild = firstCheckChildFunc(currNode)
            if(firstCheckChild) {
                const childMatch = findMatchingDomNodeAmongSiblings(
                    firstCheckChild, pred, nextSibFunc, firstCheckChildFunc)
                if(childMatch) {
                    return childMatch
                }
            }
        }
        currNode = nextSibFunc(currNode)
    }
}

/* .lastChild in function form. */
function lastChild(node) {
    return node.lastChild
}

/* .previousSibling in function form. */
function previousSibling(node) {
    return node.previousSibling
}

/* Find the next DOM node that matches matchPred in the DOM tree in the direction determined 
   by nextSibFunc. This can search the siblings and their descendants of both the provided node 
   and its parents. As it searches upward, it will be limited to the tree defined by a root node 
   that matches highestNodePred. */
function findDirectionalNearestMatchingDomNode(
    node, matchPred, highestNodePred, nextSibFunc, firstCheckChildFunc
) {
    // Start by checking siblings and their descendants.
    const sideMatch = findMatchingDomNodeAmongSiblings(
        nextSibFunc(node), matchPred, nextSibFunc, firstCheckChildFunc)
    if(sideMatch) {
        return sideMatch
    } else if(!highestNodePred(node.parentNode)) {
        /* If no match is found amongs siblings and their descendants, try the siblings and 
           descendants of the next ancestor up the DOM tree, so long as it does not match 
           highestNodePred. */
        return findDirectionalNearestMatchingDomNode(node.parentNode, matchPred,
            highestNodePred, nextSibFunc, firstCheckChildFunc)
    }
}

/* Find the nearest DOM node that matches matchPred that occurs before node in the DOM tree. 
   This can search the siblings and their descendants of both the provided node and its parents. 
   As it searches upward, it will be limited to the tree defined by a root node that matches 
   highestNodePred. */
export function findPreviousMatchingDomNode(node, matchPred, highestNodePred) {
    return findDirectionalNearestMatchingDomNode(
        node, matchPred, highestNodePred, previousSibling, lastChild)
}