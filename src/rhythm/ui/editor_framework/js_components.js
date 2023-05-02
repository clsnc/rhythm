import { createContext, createElement, useContext, useEffect, useLayoutEffect,
    useRef, useState } from 'react'

const EditableIdJsonToDomElementObjContext = createContext()

/* Represents a point in an editor. A point is any location that the caret could have. */
class EditorPoint {
    constructor(id, element, offset) {
        Object.assign(this, {id, element, offset})
    }

    /* Returns true if this references the same point in the editor as other. */
    equals(other) {
        return this.id === other.id
            && this.element === other.element
            && this.offset === other.offset
    }

    /* Returns a new EditorPoint referencing the location in the editor numSteps away 
       from this point. 1 "step" is like pressing the right arrow key once. Negative 
       steps are like pressing the left arrow key. */
    stepsAway(numSteps) {
        const newOffset = this.offset + numSteps
        return new EditorPoint(this.id, this.element, newOffset)
    }

    /* Returns whether an EditorPoint occurs before another EditorPoint. */
    static isBeforeInDocument(point0, point1) {
        const elementPosComparison = domNodePositionComparator(point0.element, point1.element)
        return elementPosComparison === 0
            ? point0.offset < point1.offset
            : elementPosComparison === 1
    }
}

/* Represents a range in the editor from startPoint (inclusive) to endPoint (exclusive). */
class EditorRange {
    constructor(anchorPoint, focusPoint) {
        const anchorIsStart = EditorPoint.isBeforeInDocument(anchorPoint, focusPoint)
        const [startPoint, endPoint] = anchorIsStart
            ? [anchorPoint, focusPoint]
            : [focusPoint, anchorPoint]
        Object.assign(this, {anchorPoint, focusPoint, startPoint, endPoint})
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

function editorPointFromIdAndOffset(idJsonToDomElementObj, id, offset) {
    const element = idJsonToDomElementObj[JSON.stringify(id)]
    return element
        ? new EditorPoint(id, element, offset)
        : null
}

/* Add .replaceRange (the suggested range to be replaced) and .afterRange (the suggested 
   range to select if the suggested change is made) to a change event. */
function addChangeRangeDataToEvent(event, replaceRange, afterRange) {
    Object.assign(event, {replaceRange, afterRange})
}

/* Adds additional data about the current selection to an event for convenience */
function addSelectionRangeToEvent(event) {
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
    event.selectionRange = new EditorRange(anchorPoint, focusPoint)
}

/* Set the selection to the given parameters if they describe a selection that is inside an editor. */
function ensureCorrectSelection(startPoint, endPoint) {
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

/* The React component for an editor root. This should be an ancestor of any EditorNode
   or Editable components. */
export function EditorRoot({onChange, onSelect, selection, ...passedDivProps}) {
    /* This object contains a mapping from JSON.stringify(editableId) -> DOM element. 
       When the selection prop is updated, the editable IDs in the prop can be used 
       with this object to access the associated DOM elements so the browser selection 
       can be updated. */
    const [idJsonToDomElementObj] = useState({})

    const {
        startId: selStartId,
        startOffset: selStartOffset,
        endId: selEndId,
        endOffset: selEndOffset
    } = selection
    const startPoint = editorPointFromIdAndOffset(idJsonToDomElementObj, selStartId, selStartOffset)
    const endPoint = editorPointFromIdAndOffset(idJsonToDomElementObj, selEndId, selEndOffset)

    /* Set the selection in the editor to whatever is described in the selection prop.
       useLayoutEffect is used here instead of useEffect to prevent caret flickering. */
    useLayoutEffect(() => {
        ensureCorrectSelection(startPoint, endPoint)
    },
        [idJsonToDomElementObj, selStartId, selStartOffset, selEndId, selEndOffset])

    // Listen for selectionchange events so the onSelect prop can be called.
    useEffect(() => {
        if(onSelect) {
            const selChangeHandler = (e) => {
                addSelectionRangeToEvent(e)
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
        divProps.onBeforeInput = (e) => {
            const afterPoint = startPoint.stepsAway(e.data.length)
            const replaceRange = new EditorRange(startPoint, endPoint)
            const afterRange = new EditorRange(afterPoint, afterPoint)
            addChangeRangeDataToEvent(e, replaceRange, afterRange)
            onChange(e)
        }
        divProps.onKeyDown = (e) => {
            if(e.key === 'Backspace') {
                e.preventDefault()
                const selHas0Len = startPoint.equals(endPoint)
                const replaceStartPoint = selHas0Len
                    ? startPoint.stepsAway(-1)
                    : startPoint
                const replaceRange = new EditorRange(replaceStartPoint, endPoint)
                const afterRange = new EditorRange(replaceStartPoint, replaceStartPoint)
                addChangeRangeDataToEvent(e, replaceRange, afterRange)
                e.data = ''
                onChange(e)
            }
        }
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