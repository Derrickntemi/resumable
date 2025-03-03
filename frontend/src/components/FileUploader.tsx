import React, { useCallback, useRef } from 'react';
import { ChunkUploader } from '../services/ChunkUploader';
import './FileUploader.css';

interface FileUploaderProps {
  onProgress: (fileId: string, progress: number) => void;
  chunkSize: number;
}

export const FileUploader: React.FC<FileUploaderProps> = ({ onProgress, chunkSize }) => {
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleFileSelect = useCallback(async (event: React.ChangeEvent<HTMLInputElement>) => {
    const files = event.target.files;
    if (!files || files.length === 0) return;

    const uploader = new ChunkUploader({
      chunkSize,
      onProgress: (progress: number) => {
        if (files[0]) {
          onProgress(files[0].name, progress);
        }
      }
    });

    for (let i = 0; i < files.length; i++) {
      try {
        await uploader.uploadFile(files[i]);
      } catch (error) {
        console.error(`Error uploading file ${files[i].name}:`, error);
        // You might want to show this error to the user in a more user-friendly way
      }
    }

    // Reset the file input
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  }, [chunkSize, onProgress]);

  const handleDragOver = useCallback((event: React.DragEvent) => {
    event.preventDefault();
    event.stopPropagation();
  }, []);

  const handleDrop = useCallback((event: React.DragEvent) => {
    event.preventDefault();
    event.stopPropagation();

    const files = event.dataTransfer.files;
    if (files && files.length > 0) {
      const input = fileInputRef.current;
      if (input) {
        // Create a new DataTransfer object and add the dropped files
        const dataTransfer = new DataTransfer();
        Array.from(files).forEach(file => dataTransfer.items.add(file));
        input.files = dataTransfer.files;
        
        // Trigger the change event handler
        const changeEvent = new Event('change', { bubbles: true });
        input.dispatchEvent(changeEvent);
      }
    }
  }, []);

  return (
    <div 
      className="file-uploader"
      onDragOver={handleDragOver}
      onDrop={handleDrop}
    >
      <div className="upload-area">
        <input
          ref={fileInputRef}
          type="file"
          multiple
          onChange={handleFileSelect}
          className="file-input"
        />
        <div className="upload-message">
          <p>Drag and drop files here</p>
          <p>or</p>
          <button 
            onClick={() => fileInputRef.current?.click()}
            className="select-files-btn"
          >
            Select Files
          </button>
        </div>
      </div>
    </div>
  );
}; 