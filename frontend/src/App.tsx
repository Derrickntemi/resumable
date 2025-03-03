import React, { useState, useCallback } from 'react';
import { FileUploader } from './components/FileUploader';
import { FileList } from './components/FileList';
import { ChunkUploader } from './services/ChunkUploader';
import './App.css';

const App: React.FC = () => {
  const [uploadProgress, setUploadProgress] = useState<{ [key: string]: number }>({});

  const handleUploadProgress = useCallback((fileId: string, progress: number) => {
    setUploadProgress(prev => ({
      ...prev,
      [fileId]: progress
    }));
  }, []);

  return (
    <div className="app">
      <header className="app-header">
        <h1>File Upload Admin Panel</h1>
      </header>
      <main className="app-main">
        <section className="upload-section">
          <h2>Upload Files</h2>
          <FileUploader
            onProgress={handleUploadProgress}
            chunkSize={8 * 1024 * 1024} // 8MB chunks
          />
        </section>
        <section className="files-section">
          <h2>Uploaded Files</h2>
          <FileList uploadProgress={uploadProgress} />
        </section>
      </main>
    </div>
  );
};

export default App; 