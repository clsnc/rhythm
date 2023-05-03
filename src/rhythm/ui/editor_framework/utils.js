import { EditorRange } from "./locations"

/* Set the selection to the given parameters if they describe a selection that is inside an editor. */
export function setDomSelection(newEditorSel) {
    if(newEditorSel) {
        const currDomSel = window.getSelection()
        const currEditorSel = EditorRange.fromDomRange(currDomSel)
        // Only change the selection if it is different from the new one.
        if(!currEditorSel.equals(newEditorSel)) {
            currDomSel.removeAllRanges()
            currDomSel.addRange(newEditorSel.toDomRange())
        }
    }
}