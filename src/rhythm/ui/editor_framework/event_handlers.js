import { EditorRange, addChangeRangeDataToEvent, editorRangeUnorderedEquals, getEditorSelection } from "./locations"

export function handleBeforeInput(editorRange, event, onChange) {
    const {startPoint, endPoint} = editorRange
    const afterPoint = startPoint.stepsAway(event.data.length)
    const replaceRange = new EditorRange(startPoint, endPoint)
    const afterRange = new EditorRange(afterPoint, afterPoint)
    addChangeRangeDataToEvent(event, replaceRange, afterRange)
    onChange(event)
}

export function handleKeyDown(editorRange, event, onChange) {
    const {startPoint, endPoint} = editorRange
    if(event.key === 'Backspace') {
        event.preventDefault()
        /* If the selection has 0 length, the range to be replaced should start with the character
           before the caret to simulate normal backspace behavior when nothing is highlighted. */
        const selHas0Len = startPoint.equals(endPoint)
        const replaceStartPoint = selHas0Len
            ? startPoint.stepsAway(-1)
            : startPoint
        const replaceRange = new EditorRange(replaceStartPoint, endPoint)
        const afterRange = new EditorRange(replaceStartPoint, replaceStartPoint)
        addChangeRangeDataToEvent(event, replaceRange, afterRange)
        event.data = ''
        onChange(event)
    }
}

export function handleSelectionChange(event, providedEditorSelection, onSelect) {
    const currEditorSelection = getEditorSelection(event)
    /* Only call onSelect if the new selection is different from the provided one. This 
       prevents an endless loop of selection change events. */
    if(!editorRangeUnorderedEquals(currEditorSelection, providedEditorSelection)) {
        event.selectionRange = currEditorSelection
        onSelect(event)
    }
}