/* The classes in this file are declared with "const <ClassName> = class <ClassName>" because 
   they do not seem to get referenced correctly from other files with just "class <ClassName>". */

import { domNodePositionComparator, editorOffsetFromDomOffset } from "./utils"

/* Represents a point in an editor. A point is any location that the caret could have. */
export const EditorPoint = class EditorPoint {
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

    /* Returns a new EditorPoint if the given ID is an editable; otherwise returns null. */
    static fromIdAndOffset(idJsonToDomElementObj, id, offset) {
        const element = idJsonToDomElementObj[JSON.stringify(id)]
        return element
            ? new EditorPoint(id, element, offset)
            : null
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
export const EditorRange = class EditorRange {
    constructor(anchorPoint, focusPoint) {
        const anchorIsStart = EditorPoint.isBeforeInDocument(anchorPoint, focusPoint)
        const [startPoint, endPoint] = anchorIsStart
            ? [anchorPoint, focusPoint]
            : [focusPoint, anchorPoint]
        Object.assign(this, {anchorPoint, focusPoint, startPoint, endPoint})
    }
}

/* Add .replaceRange (the suggested range to be replaced) and .afterRange (the suggested 
   range to select if the suggested change is made) to a change event. */
export function addChangeRangeDataToEvent(event, replaceRange, afterRange) {
    Object.assign(event, {replaceRange, afterRange})
}

/* Adds additional data about the current selection to an event for convenience */
export function addSelectionRangeToEvent(event) {
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