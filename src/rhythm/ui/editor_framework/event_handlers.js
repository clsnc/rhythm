import { EditorRange, addChangeRangeDataToEvent, addSelectionRangeToEvent } from "./locations"

export function handleBeforeInput(startPoint, endPoint, event, onChange) {
    const afterPoint = startPoint.stepsAway(event.data.length)
    const replaceRange = new EditorRange(startPoint, endPoint)
    const afterRange = new EditorRange(afterPoint, afterPoint)
    addChangeRangeDataToEvent(event, replaceRange, afterRange)
    onChange(event)
}

export function handleKeyDown(startPoint, endPoint, event, onChange) {
    if(event.key === 'Backspace') {
        event.preventDefault()
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

export function handleSelectionChange(event, onSelect) {
    addSelectionRangeToEvent(event)
    onSelect(event)
}