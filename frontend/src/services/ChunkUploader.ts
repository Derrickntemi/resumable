interface ChunkUploaderOptions {
  chunkSize: number;
  onProgress: (progress: number) => void;
}

export class ChunkUploader {
  private chunkSize: number;
  private onProgress: (progress: number) => void;

  constructor(options: ChunkUploaderOptions) {
    this.chunkSize = options.chunkSize;
    this.onProgress = options.onProgress;
  }

  async uploadFile(file: File): Promise<void> {
    const totalChunks = Math.ceil(file.size / this.chunkSize);
    const uploadId = crypto.randomUUID();
    let uploadedChunks = 0;

    for (let chunkNumber = 0; chunkNumber < totalChunks; chunkNumber++) {
      const start = chunkNumber * this.chunkSize;
      const end = Math.min(start + this.chunkSize, file.size);
      const chunk = file.slice(start, end);

      try {
        await this.uploadChunk(chunk, uploadId, chunkNumber, totalChunks);
        uploadedChunks++;
        this.onProgress((uploadedChunks / totalChunks) * 100);
      } catch (error) {
        console.error(`Error uploading chunk ${chunkNumber}:`, error);
        throw error;
      }
    }
  }

  private async uploadChunk(
    chunk: Blob,
    uploadId: string,
    chunkNumber: number,
    totalChunks: number
  ): Promise<void> {
    const formData = new FormData();
    formData.append('file', chunk);

    const response = await fetch('/api/v1/files/upload', {
      method: 'POST',
      headers: {
        'X-Upload-ID': uploadId,
        'X-Chunk-Number': chunkNumber.toString(),
        'X-Total-Chunks': totalChunks.toString()
      },
      body: formData
    });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const result = await response.json();
    return result;
  }
} 