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

    static fromDomElementAndOffset(element, offset) {
        // Account for placeholder text in the DOM.
        const editableOffset = editorOffsetFromDomOffset(offset, element)
        const editableId = element.editableId
        return editableId
            ? new EditorPoint(editableId, element, editableOffset)
            : null
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

    /* Returns a DOM Range object referencing the editable elements covered by this EditorRange. */
    toDomRange() {
        const {
            anchorPoint: {
                element: anchorElement,
                offset: anchorOffset
            },
            focusPoint: {
                element: focusElement,
                offset: focusOffset
            }
        } = this
        const domRange = document.createRange()
        // .firstChild is used below to reference the text node inside of the editable element.
        domRange.setStart(anchorElement.firstChild, anchorOffset)
        domRange.setEnd(focusElement.firstChild, focusOffset)
        return domRange
    }

    /* Returns a new EditorRange covering the editables referenced by the DOM Range object. */
    static fromDomRange(domRange) {
        const {
            anchorNode,
            anchorOffset: domAnchorOffset,
            focusNode,
            focusOffset: domFocusOffset
        } = domRange
        const anchorElement = anchorNode.parentElement
        const focusElement = focusNode.parentElement
        const anchorPoint = EditorPoint.fromDomElementAndOffset(anchorElement, domAnchorOffset)
        const focusPoint = EditorPoint.fromDomElementAndOffset(focusElement, domFocusOffset)
        return anchorPoint && focusPoint
            ? new EditorRange(anchorPoint, focusPoint)
            : null
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
    event.selectionRange = EditorRange.fromDomRange(selection)
}