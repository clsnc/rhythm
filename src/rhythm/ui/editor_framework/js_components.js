import { createContext, createElement, useContext, useEffect, useRef, useState } from 'react'

const EditableIdJsonToDomElementObjContext = createContext()

/* Represents a point in an editor. A point is any location that the caret could have. */
class EditorPoint {
    constructor(id, element, offset) {
        Object.assign(this, {id, element, offset})
    }

    /* Returns whether an EditorPoint occurs before another EditorPoint. */
    static isBeforeInDocument(point0, point1) {
        const elementPosComparison = domNodePositionComparator(point0.element, point1.element)
        return elementPosComparison === 0
            ? point0.offset < point1.offset
            : elementPosComparison === 1
    }
}

/* A position comparator for DOM nodes in the document */
function domNodePositionComparator(n0, n1) {
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
function editorOffsetFromDomOffset(domOffset, element) {
    return Math.min(domOffset, element.editorValue.length)
}

/* Adds additional data about the current selection to an event for convenience */
function addStartAndEndPointDataToEvent(event) {
    const selection = window.getSelection()
    const {
        anchorNode,
        anchorOffset: domAnchorOffset,
        focusNode,
        focusOffset: domFocusOffset
    } = selection
    const anchorElement = anchorNode.parentElement
    const focusElement = focusNode.parentElement

    // Account for placeholder text in the DOM.
    const anchorOffset = editorOffsetFromDomOffset(domAnchorOffset, anchorElement)
    const focusOffset = editorOffsetFromDomOffset(domFocusOffset, focusElement)

    const anchorEditableId = anchorElement.editableId
    const focusEditableId = focusElement.editableId
    const anchorPoint = new EditorPoint(anchorEditableId, anchorElement, anchorOffset)
    const focusPoint = new EditorPoint(focusEditableId, focusElement, focusOffset)
    const anchorIsStart = EditorPoint.isBeforeInDocument(anchorPoint, focusPoint)
    const [startPoint, endPoint] = anchorIsStart
        ? [anchorPoint, focusPoint]
        : [focusPoint, anchorPoint]
    Object.assign(event, {
        anchorEditableId: anchorEditableId, focusEditableId: focusEditableId,
        startEditableId: startPoint.id,
        startElement: startPoint.element,
        startOffset: startPoint.offset,
        endEditableId: endPoint.id,
        endElement: endPoint.element,
        endOffset: endPoint.offset
    })
}

/* Set the selection to the given parameters if they describe a selection that is inside an editor. */
function ensureCorrectSelection(idJsonToDomElementObj, startId, startOffset, endId, endOffset) {
    const startElement = idJsonToDomElementObj[JSON.stringify(startId)]
    const endElement = idJsonToDomElementObj[JSON.stringify(endId)]

    if(startElement && endElement) {
        const newSelRange = document.createRange()
        newSelRange.setStart(startElement.firstChild, startOffset)
        newSelRange.setEnd(endElement.firstChild, endOffset)
        const currSel = window.getSelection()
        currSel.removeAllRanges()
        currSel.addRange(newSelRange)
    }
}

/* The React component for an editor root. This should be an ancestor of any EditorNode
   or Editable components. */
export function EditorRoot({onChange, onSelect, selection, ...passedDivProps}) {
    const {
        startId: selStartId,
        startOffset: selStartOffset,
        endId: selEndId,
        endOffset: selEndOffset
    } = selection
    
    /* This object contains a mapping from JSON.stringify(editableId) -> DOM element. 
       When the selection prop is updated, the editable IDs in the prop can be used 
       with this object to access the associated DOM elements so the browser selection 
       can be updated. */
    const [idJsonToDomElementObj] = useState({})

    // Set the selection in the editor to whatever is described in the selection prop.
    useEffect(() => ensureCorrectSelection(idJsonToDomElementObj,
            selStartId, selStartOffset, selEndId, selEndOffset),
        [idJsonToDomElementObj, selStartId, selStartOffset, selEndId, selEndOffset])

    // Listen for selectionchange events so the onSelect prop can be called.
    useEffect(() => {
        if(onSelect) {
            const selChangeHandler = (e) => {
                addStartAndEndPointDataToEvent(e)
                onSelect(e)
            }
            document.addEventListener('selectionchange', selChangeHandler)
            return () => document.removeEventListener('selectionchange', selChangeHandler)
        }
    }, [onSelect])

    const divProps = {
        ...passedDivProps,
        contentEditable: true,
        suppressContentEditableWarning: true
    }

    if(onChange) {
        divProps.onBeforeInput = onChange
    }

    return createElement(
        EditableIdJsonToDomElementObjContext.Provider,
        {value: idJsonToDomElementObj},
        createElement('div', divProps)
    )
}

/* A React component for an editable component inside an editor */
export function Editable({editableId, value, ...divProps}) {
    const idJsonToDomElementObj = useContext(EditableIdJsonToDomElementObjContext)
    const elementRef = useRef()

    /* Storing the editable ID in the DOM element allows it to be accessed by 
       event handlers. */
    useEffect(() => {
        const element = elementRef.current
        element.editableId = editableId
        const jsonEditableId = JSON.stringify(editableId)
        idJsonToDomElementObj[jsonEditableId] = element
        return () => delete idJsonToDomElementObj[jsonEditableId]
    }, [elementRef.current, editableId, idJsonToDomElementObj])

    /* Storing the intended editable value in the DOM element allows it to be 
       accessed by event handlers. */
    useEffect(() => elementRef.current.editorValue = value)

    /* If there is no text to be rendered, render a zero width space so there 
       is still a text node in the DOM for the browser to focus. */
    const domValue = value.length > 0 ? value : "\u200B"

    return createElement('div', {
        ...divProps,
        ref: elementRef
    }, domValue)
}