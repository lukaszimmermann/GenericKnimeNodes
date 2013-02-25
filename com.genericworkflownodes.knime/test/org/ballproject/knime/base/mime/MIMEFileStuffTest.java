package org.ballproject.knime.base.mime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.ballproject.knime.base.mime.mocks.MockMIMEFileCell;
import org.junit.Test;

import com.genericworkflownodes.knime.test.data.TestDataSource;
import com.genericworkflownodes.util.Helper;

public class MIMEFileStuffTest {

	@Test
	public void test1() throws IOException {
		String tmpfile1 = Helper.getTemporaryFilename("unk", true);
		Helper.copyStream(TestDataSource.class.getResourceAsStream("test.unk"),
				new File(tmpfile1));

		String tmpfile2 = Helper.getTemporaryFilename("ctd", true);
		Helper.copyStream(
				TestDataSource.class.getResourceAsStream("test2.ctd"),
				new File(tmpfile2));

		MockMIMEFileCell mfc1 = new MockMIMEFileCell();
		mfc1.read(new File(tmpfile1));
		assertEquals(mfc1.toString(), "UNKMimeFileCell");

		MockMIMEFileCell mfc2 = new MockMIMEFileCell();
		mfc2.read(new File(tmpfile1));

		assertTrue(mfc1.equals(mfc2));

		MockMIMEFileCell mfc3 = new MockMIMEFileCell();
		mfc3.read(new File(tmpfile2));

		assertEquals(mfc1.getExtension(), "unk");

		assertFalse(mfc1.equals(mfc3));
		assertFalse(mfc2.equals(mfc3));

		assertEquals(mfc1.hashCode(), mfc2.hashCode());
		assertTrue(mfc1.hashCode() != mfc3.hashCode());
		assertTrue(mfc2.hashCode() != mfc3.hashCode());

	}

}
