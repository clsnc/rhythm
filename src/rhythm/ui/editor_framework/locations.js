/* The classes in this file are declared with "const <ClassName> = class <ClassName>" because 
   they do not seem to get referenced correctly from other files with just "class <ClassName>". */

import { findPreviousMatchingDomNode } from "./dom_search"

function isEditable(element) {
    return Boolean(element.isEditable)
}

function isEditor(element) {
    return Boolean(element.isEditor)
}

/* Returns whether a DOM element is styled to be inline. */
function isInline(element) {
    return window.getComputedStyle(element).display === "inline"
}

/* Represents a point in an editor. A point is any location that the caret could have. */
export const EditorPoint = class EditorPoint {
    constructor(idJsonToDomElementObj, id, offset) {
        const idStr = JSON.stringify(id)
        Object.assign(this, {idJsonToDomElementObj, id, idStr, offset})
    }

    /* Returns the DOM element that currently has the editable ID of this EditorPoint. If there 
       is none currently, undefined is returned. */
    currentElement() {
        return this.idJsonToDomElementObj[this.idStr]
    }

    /* Returns true if the EditorPoint currently exists in the editor. */
    currentlyExists() {
        return Boolean(this.currentElement())
    }

    /* Returns a normalized point. If two inline Editables are adjacent, then the last position
       of the first is no different than the first position of the second. In this case, the 
       last position of the second is returned. */
    normalize() {
        const currElement = this.currentElement()
        if(this.offset === 0 && isInline(currElement)) {
            const prevElement = currElement.previousSibling
            if(prevElement && isEditable(prevElement) && isInline(prevElement)) {
                /* If the offset is 0 and both this element and its previous sibling 
                   are inline, return a point at the end of the previous sibling. */
                return EditorPoint.fromDomElementAndOffset(
                    prevElement, prevElement.editorValue.length)
            }
        }
        return this
    }

    /* Returns true if this references the same point in the editor as other. */
    equals(other) {
        return this.idJsonToDomElementObj === other.idJsonToDomElementObj
            && this.idStr === other.idStr
            && this.offset === other.offset
    }

    /* Returns a new EditorPoint referencing the location in the editor numSteps away 
       from this point. 1 "step" is like pressing the right arrow key once. Negative 
       steps are like pressing the left arrow key. */
    stepsAway(numSteps) {
        const newOffset = this.offset + numSteps
        if(newOffset >= 0) {
            // For 0 or positive offsets, return a new point inside the current Editable.
            return new EditorPoint(this.idJsonToDomElementObj, this.id, newOffset)
        } else {
            // For negative offsets, return a point at the end of the previous Editable.
            const prevEditable = findPreviousMatchingDomNode(
                this.currentElement(), isEditable, isEditor)
            return prevEditable
                ? EditorPoint.fromDomElementAndOffset(prevEditable, prevEditable.editorValue.length)
                /* If there is no previous Editable, return a point at the beginning of the current 
                   Editable. */
                : new EditorPoint(this.idJsonToDomElementObj, this.id, 0)
        }
    }

    static fromDomElementAndOffset(element, offset) {
        const {idJsonToDomElementObj, editableId} = element
        if(editableId) {
            // Account for placeholder text in the DOM.
            const editableOffset = editorOffsetFromDomOffset(offset, element)
            return new EditorPoint(idJsonToDomElementObj, editableId, editableOffset)
        } else {
            return null
        }
    }

    /* Returns a new EditorPoint. */
    static fromIdAndOffset(idJsonToDomElementObj, id, offset) {
        return new EditorPoint(idJsonToDomElementObj, id, offset)
    }

    /* Returns whether an EditorPoint occurs before another EditorPoint. */
    static isBeforeInDocument(point0, point1) {
        const elementPosComparison = domNodePositionComparator(point0.currentElement(), point1.currentElement())
        return elementPosComparison === 0
            ? point0.offset < point1.offset
            : elementPosComparison === 1
    }
}

/* Represents a range in the editor from startPoint (inclusive) to endPoint (exclusive). */
export const EditorRange = class EditorRange {
    constructor(anchorPoint, focusPoint) {
        const pointsAreSame = anchorPoint.equals(focusPoint)
        const anchorIsStart = EditorPoint.isBeforeInDocument(anchorPoint, focusPoint)
        const [startPoint, endPoint] = anchorIsStart
            ? [anchorPoint, focusPoint]
            : [focusPoint, anchorPoint]
        Object.assign(this, {anchorPoint, focusPoint, startPoint, endPoint, pointsAreSame})
    }

    /* Returns true if both the start and end EditorPoints currently exist in order in the editor. */
    currentlyExists() {
        return this.anchorPoint.currentlyExists()
            && (this.pointsAreSame
                || (this.focusPoint.currentlyExists()
                    && EditorPoint.isBeforeInDocument(this.startPoint, this.endPoint)))
    }

    /* Returns true if this references the same range (with the same anchor and focus points) 
       in the editor as other. */
    equals(other) {
        return this.anchorPoint.equals(other.anchorPoint)
            && this.focusPoint.equals(other.focusPoint)
    }

    /* Returns a normalized EditorRange. */
    normalize() {
        return new EditorRange(this.anchorPoint.normalize(), this.focusPoint.normalize())
    }

    /* Returns true if this references the same range in the editor as other without
       considering the order of the anchor and focus points. */
    unorderedEquals(other) {
        return other
            ? this.startPoint.equals(other.startPoint)
                && this.endPoint.equals(other.endPoint)
            : false
    }

    /* Returns a DOM Range object referencing the editable elements covered by this EditorRange. */
    toDomRange() {
        const anchorElement = this.anchorPoint.currentElement()
        const focusElement = this.focusPoint.currentElement()
        const anchorOffset = this.anchorPoint.offset
        const focusOffset = this.focusPoint.offset
        const domRange = document.createRange()
        // .firstChild is used below to reference the text node inside of the editable element.
        domRange.setStart(anchorElement.firstChild, anchorOffset)
        domRange.setEnd(focusElement.firstChild, focusOffset)
        return domRange
    }

    /* Returns an object containing the range IDs and offsets. This matches the format of 
       selection ranges provided to the editor as props. */
    toExportRange() {
        return {
            startId: this.startPoint.id,
            startOffset: this.startPoint.offset,
            endId: this.endPoint.id,
            endOffset: this.endPoint.offset
        }
    }

    /* Returns a new EditorRange covering the editables referenced by the DOM Range object. */
    static fromDomRange(domRange) {
        const {
            anchorNode,
            anchorOffset: domAnchorOffset,
            focusNode,
            focusOffset: domFocusOffset
        } = domRange

        // Handle domRange having null nodes (usually because there is no selection).
        if(anchorNode && focusNode) {
            const anchorElement = anchorNode.parentElement
            const focusElement = focusNode.parentElement
            const anchorPoint = EditorPoint.fromDomElementAndOffset(anchorElement, domAnchorOffset)
            const focusPoint = EditorPoint.fromDomElementAndOffset(focusElement, domFocusOffset)
            return anchorPoint && focusPoint
                ? new EditorRange(anchorPoint, focusPoint)
                : null
        } else {
            return null
        }
    }
}

/* Add .replaceRange (the suggested range to be replaced) and .afterRange (the suggested 
   range to select if the suggested change is made) as export ranges to a change event. */
export function addChangeRangeDataToEvent(event, replaceRange, afterRange) {
    event.replaceRange = replaceRange.toExportRange()
    event.afterRange = afterRange.toExportRange()
}

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

/* Returns the current window selection as a normalized editor selection. */
export function getEditorSelection() {
    const selection = window.getSelection()
    return EditorRange.fromDomRange(selection).normalize()
}

/* Similar to x.unorderedEquals(y), except that a nullish x is handled smoothly. */
export function editorRangeUnorderedEquals(a, b) {
    return a == null
        ? b == null
        : a.unorderedEquals(b)
}