import { EditorRange } from "./locations"

/* Set the selection to the given parameters if they describe a selection that is inside an editor. */
export function setDomSelection(newEditorSel) {
    const currDomSel = window.getSelection()
    const currEditorSel = EditorRange.fromDomRange(currDomSel)
    /* Only change the selection if it is different from the new one. Because DOM ranges 
       have start and end nodes rather than anchor and focus nodes, check only that the 
       current selection covers the same range as the new one. */
    if(!(currEditorSel && currEditorSel.unorderedEquals(newEditorSel))) {
        currDomSel.removeAllRanges()
        currDomSel.addRange(newEditorSel.toDomRange())
    }
}