import { createContext, createElement, Fragment, useContext, useEffect, useRef } from 'react'

const EditorPathContext = createContext()

/* Represents a point in an editor. A point is any location that the caret could have. */
class EditorPoint {
    constructor(path, element, offset) {
        Object.assign(this, {path, element, offset})
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

/* Adds additional data about the current selection to an event for convenience */
function addStartAndEndPointDataToEvent(event) {
    const selection = window.getSelection()
    const {anchorNode, anchorOffset, focusNode, focusOffset} = selection
    const anchorElement = anchorNode.parentElement
    const focusElement = focusNode.parentElement
    const anchorEditorPath = anchorElement.editorPath
    const focusEditorPath = focusElement.editorPath
    const anchorPoint = new EditorPoint(anchorEditorPath, anchorElement, anchorOffset)
    const focusPoint = new EditorPoint(focusEditorPath, focusElement, focusOffset)
    const anchorIsStart = EditorPoint.isBeforeInDocument(anchorPoint, focusPoint)
    const [startPoint, endPoint] = anchorIsStart
        ? [anchorPoint, focusPoint]
        : [focusPoint, anchorPoint]
    Object.assign(event, {
        anchorEditorPath, focusEditorPath,
        startEditorPath: startPoint.path,
        startElement: startPoint.element,
        startOffset: startPoint.offset,
        endEditorPath: endPoint.path,
        endElement: endPoint.element,
        endOffset: endPoint.offset
    })
}

/* Handles an onBeforeInput event on an editor */
function handleBeforeInput(event, handleChange) {
    addStartAndEndPointDataToEvent(event)
    handleChange(event)
}

/* The React component for an editor root. This should be an ancestor of any EditorNode
   or Editable components. */
export function EditorRoot({onChange, ...divProps}) {
    return createElement(
        EditorPathContext.Provider,
        {value: []},
        createElement(
            'div',
            {
                ...divProps,
                contentEditable: true,
                suppressContentEditableWarning: true,
                onBeforeInput: (e) => handleBeforeInput(e, onChange)
            }
        )
    )
}

/* A React component for representing editor nodes in the editor paths contained in 
   onChange events. This accepts children but does not by itself cause anything to 
   be added to the DOM. */
export function EditorNode({editorId, children}) {
    const editorParentPath = useContext(EditorPathContext)
    const editorPath = [...editorParentPath, editorId]
    return createElement(
        EditorPathContext.Provider,
        {value: editorPath},
        createElement(Fragment, {children})
    )
}

/* A React component for an editable component inside an editor */
export function Editable({value, ...divProps}) {
    const editorPath = useContext(EditorPathContext)
    const elementRef = useRef()
    useEffect(
        // Storing the editor path in the DOM element allows it to be accessed by 
        // event handlers.
        () => elementRef.current.editorPath = editorPath,
        [elementRef.current, editorPath]
    )
    return createElement(
        EditorPathContext.Provider,
        {value: editorPath},
        createElement('div', {
            ...divProps,
            ref: elementRef
        }, value)
    )
}