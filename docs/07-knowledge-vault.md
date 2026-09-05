# Knowledge Vault

The Knowledge Vault is MindQ's personal study-material repository.

## Supported Inputs

- PDF
- DOCX
- pasted text

## Upload Pipeline

```text
Upload
 ↓
File validation
 ↓
Storage quota validation
 ↓
Text extraction
 ↓
StudyMaterial persistence
 ↓
Available for AI generation
```

## PDF

Processed with Apache PDFBox.

## DOCX

Processed with Apache POI.

## Storage

The free-plan quota is:

**500 MB per user**

Storage usage is displayed to the user and enforced server-side.

The current implementation stores extracted text and material metadata in MySQL rather than retaining the original binary upload in object storage.

## User Operations

- create
- upload
- list
- search
- view
- edit
- delete
- use as AI input

## AI Integration

A Vault material can be used to generate MCQs and other supported AI learning outputs.
