import { createContext, createElement, useContext, useEffect, useLayoutEffect,
    useRef, useState } from 'react'

import { EditorPoint, EditorRange } from './locations'
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
    /* These points cannot be normalized yet because normalization requires looking at the DOM which
       has not been updated yet. */
    const selStartNotNorm = EditorPoint.fromIdAndOffset(idJsonToDomElementObj, selStartId, selStartOffset)
    const selEndNotNorm = EditorPoint.fromIdAndOffset(idJsonToDomElementObj, selEndId, selEndOffset)
    const selRangeNotNorm = new EditorRange(selStartNotNorm, selEndNotNorm)
    const elementRef = useRef()

    // Leave a marker on the editor root DOM element so it can be identified.
    useEffect(() => { elementRef.current.isEditor = true })

    /* Set the selection in the editor to whatever is described in the selection prop.
       useLayoutEffect is used here instead of useEffect to prevent caret flickering. */
    useLayoutEffect(() => {
        const selRange = selRangeNotNorm.normalize()
        if(selRange.currentlyExists()) {
            setDomSelection(selRange)
        }
    }, [idJsonToDomElementObj, selRangeNotNorm])

    // Listen for selectionchange events so the onSelect prop can be called.
    useEffect(() => {
        if(onSelect) {
            const selChangeHandler = (e) => handleSelectionChange(e, selRangeNotNorm, onSelect)
            document.addEventListener('selectionchange', selChangeHandler)
            return () => { document.removeEventListener('selectionchange', selChangeHandler) }
        }
    }, [selRangeNotNorm, onSelect])

    const divProps = {
        ...passedDivProps,
        ref: elementRef,
        contentEditable: true,
        suppressContentEditableWarning: true
    }

    if(onChange) {
        divProps.onBeforeInput = (e) => handleBeforeInput(selRangeNotNorm, e, onChange)
        divProps.onKeyDown = (e) => handleKeyDown(selRangeNotNorm, e, onChange)
    }

    return createElement(
        EditableIdJsonToDomElementObjContext.Provider,
        {value: idJsonToDomElementObj},
        createElement('div', divProps)
    )
}

/* A React component for an editable component inside an editor */
export function Editable({editableId, value, ...divProps}) {
    /* When useLayoutEffect is used instead of useEffect in this function, it is 
       because these Effects must run before the LayoutEffect of the ancestor Editor 
       component that updates the window selection. */

    const idJsonToDomElementObj = useContext(EditableIdJsonToDomElementObjContext)
    const elementRef = useRef()

    /* Storing the editable ID and ID -> element map object in the DOM element allows 
       an EditorPoint to be derived from a DOM element and an offset. */
    useLayoutEffect(() => {
        const element = elementRef.current
        Object.assign(element, {idJsonToDomElementObj, editableId})
        const jsonEditableId = JSON.stringify(editableId)
        idJsonToDomElementObj[jsonEditableId] = element
        return () => { delete idJsonToDomElementObj[jsonEditableId] }
    }, [elementRef.current, editableId, idJsonToDomElementObj])

    /* Storing the intended editable value in the DOM element allows it to be 
       accessed by event handlers. */
    useLayoutEffect(() => { elementRef.current.editorValue = value }, [elementRef.current, value])

    // Leave a marker on the Editable DOM element so it can be identified.
    useEffect(() => { elementRef.current.isEditable = true })

    /* If there is no text to be rendered, render a zero width space so there 
       is still a text node in the DOM for the browser to focus. */
    const domValue = value.length > 0 ? value : "\u200B"

    return createElement('div', {
        ...divProps,
        ref: elementRef
    }, domValue)
}