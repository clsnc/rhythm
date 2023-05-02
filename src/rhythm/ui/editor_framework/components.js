import { createContext, createElement, useContext, useEffect, useLayoutEffect,
    useRef, useState } from 'react'

import { EditorPoint } from './locations'
import { setDomSelection } from './utils'
import { handleBeforeInput, handleKeyDown, handleSelectionChange } from './event_handlers'

const EditableIdJsonToDomElementObjContext = createContext()

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
    const startPoint = EditorPoint.fromIdAndOffset(idJsonToDomElementObj, selStartId, selStartOffset)
    const endPoint = EditorPoint.fromIdAndOffset(idJsonToDomElementObj, selEndId, selEndOffset)

    /* Set the selection in the editor to whatever is described in the selection prop.
       useLayoutEffect is used here instead of useEffect to prevent caret flickering. */
    useLayoutEffect(() => setDomSelection(startPoint, endPoint),
        [idJsonToDomElementObj, selStartId, selStartOffset, selEndId, selEndOffset])

    // Listen for selectionchange events so the onSelect prop can be called.
    useEffect(() => {
        if(onSelect) {
            const selChangeHandler = (e) => handleSelectionChange(e, onSelect)
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
        divProps.onBeforeInput = (e) => handleBeforeInput(startPoint, endPoint, e, onChange)
        divProps.onKeyDown = (e) => handleKeyDown(startPoint, endPoint, e, onChange)
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