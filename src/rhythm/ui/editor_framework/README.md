## Rhythm Editor Framework
This is an experimental React editor framework for use in Rhythm. It is higher-level than HTML contenteditable elements (from which it is built) but lower-level than most existing React-compatible editor frameworks. If this turns out to be a good way to build text editors in React, it will probably make sense to release it as a separate package for use in other projects.

### Why this instead of an existing framework?
It is less of a drop-in solution than most other editor libraries, but has a few key advantages:
- It is designed specifically for use in React projects.
- Editor content and selection are controlled. It has no internal state that your code needs to sync with.
- It does not mandate use of an internal data model; it just that each Editable element has a unique identifier within an Editor.
- Editor content is expressed with normal React components:
  ```
  <EditorRoot>
    <div>Some label text</div>
    <Editable value="edit me" editableId={editorTreePath1}/>
    <div>
      <SomeOtherComponent/>
      <Editable value="edit me 2" editableId={editorTreePath2}/>
    </div>
  </EditorRoot>
  ```
