import React, { useEffect, useState } from 'react';
import './FileList.css';

interface FileListProps {
  uploadProgress: { [key: string]: number };
}

interface FileInfo {
  id: string;
  filename: string;
  size: number;
  uploadedAt: string;
  contentType: string;
}

export const FileList: React.FC<FileListProps> = ({ uploadProgress }) => {
  const [files, setFiles] = useState<FileInfo[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchFiles = async () => {
    try {
      const response = await fetch('/api/v1/files');
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      const data = await response.json();
      setFiles(data);
      setError(null);
    } catch (err) {
      setError('Failed to load files');
      console.error('Error fetching files:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchFiles();
    const interval = setInterval(fetchFiles, 5000); // Refresh every 5 seconds
    return () => clearInterval(interval);
  }, []);

  const formatFileSize = (bytes: number): string => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return `${parseFloat((bytes / Math.pow(k, i)).toFixed(2))} ${sizes[i]}`;
  };

  const handleDownload = async (fileId: string, filename: string) => {
    try {
      const response = await fetch(`/api/v1/files/download/${fileId}`);
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = filename;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
    } catch (err) {
      console.error('Error downloading file:', err);
      alert('Failed to download file');
    }
  };

  if (loading) {
    return <div className="file-list loading">Loading...</div>;
  }

  if (error) {
    return <div className="file-list error">{error}</div>;
  }

  return (
    <div className="file-list">
      {files.length === 0 ? (
        <div className="no-files">No files uploaded yet</div>
      ) : (
        <table>
          <thead>
            <tr>
              <th>Filename</th>
              <th>Size</th>
              <th>Upload Date</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {files.map(file => (
              <tr key={file.id}>
                <td>{file.filename}</td>
                <td>{formatFileSize(file.size)}</td>
                <td>{new Date(file.uploadedAt).toLocaleString()}</td>
                <td>
                  {uploadProgress[file.filename] !== undefined ? (
                    <div className="progress-bar">
                      <div 
                        className="progress"
                        style={{ width: `${uploadProgress[file.filename]}%` }}
                      />
                      <span>{Math.round(uploadProgress[file.filename])}%</span>
                    </div>
                  ) : (
                    'Completed'
                  )}
                </td>
                <td>
                  <button
                    onClick={() => handleDownload(file.id, file.filename)}
                    disabled={uploadProgress[file.filename] !== undefined}
                  >
                    Download
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}; 