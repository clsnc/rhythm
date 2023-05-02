/* A position comparator for DOM nodes in the document */
export function domNodePositionComparator(n0, n1) {
    return n0 === n1
        ? 0
        : n0.compareDocumentPosition(n1) & Node.DOCUMENT_POSITION_FOLLOWING
            ? 1
            : -1
}

/* Computes what the offset would be in an element if it contained only the content 
   provided to the editor in props. The content of the DOM element may be different 
   than the provided content if there is some kind of placeholder text, such as a 
   zero width space to improve focus behavior. */
export function editorOffsetFromDomOffset(domOffset, element) {
    return Math.min(domOffset, element.editorValue.length)
}

/* Set the selection to the given parameters if they describe a selection that is inside an editor. */
export function setDomSelection(startPoint, endPoint) {
    if(startPoint && endPoint) {
        const {element: startElement, offset: startOffset} = startPoint
        const {element: endElement, offset: endOffset} = endPoint
        const newSelRange = document.createRange()
        newSelRange.setStart(startElement.firstChild, startOffset)
        newSelRange.setEnd(endElement.firstChild, endOffset)
        const currSel = window.getSelection()
        currSel.removeAllRanges()
        currSel.addRange(newSelRange)
    }
}